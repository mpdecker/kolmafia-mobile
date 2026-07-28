package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.ItemDatabase

/** Desktop [ShopRow.parseShop] HTML inventory parsing for shop.php visits. */
object ShopRowParser {

    data class ParsedSingleCostRow(
        val rowId: Int,
        val itemId: Int,
        val itemName: String,
        val currencyName: String,
        val price: Int,
    )

    private val ROW_PATTERN = Regex(
        """<tr rel="(\d+)">(.*?)</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val SINGLE_COST_PATTERN = Regex(
        """<tr rel="(\d+)">.*?onClick='javascript:descitem\((\d+)\)'>.*?<b>(.*?)</b>.*?title="(.*?)".*?<b>([\d,]+)</b>.*?whichrow=(\d+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val TD_PATTERN = Regex(
        """<td(.*?)>(.*?)</td>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val TD2_PATTERN = Regex(
        """itemimages/(.*?)\..*?descitem\((\d*)\)""",
        RegexOption.IGNORE_CASE,
    )

    private val TD3_PATTERN = Regex("""<b>\(?(.*?)\)?</b>""")
    private val IODD_PATTERN = Regex("""<b>([\d,]+)</b>""")
    private val IROW_PATTERN = Regex("""whichrow=(\d+)""")

    fun parseShop(html: String, includeMeat: Boolean = true): List<ShopRow> {
        val byRowId = linkedMapOf<Int, ShopRow>()

        for (match in SINGLE_COST_PATTERN.findAll(html)) {
            val itemId = match.groupValues[1].toIntOrNull() ?: continue
            val currencyName = match.groupValues[4].trim()
            val price = match.groupValues[5].replace(",", "").toIntOrNull() ?: continue
            val rowId = match.groupValues[6].toIntOrNull() ?: continue
            parseSingleCostRow(itemId, currencyName, price, rowId)?.let { byRowId[rowId] = it }
        }

        for (match in ROW_PATTERN.findAll(html)) {
            val relItemId = match.groupValues[1].toIntOrNull() ?: continue
            val rowBlock = match.groupValues[2]
            val rowId = IROW_PATTERN.find(rowBlock)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            if (byRowId.containsKey(rowId)) continue
            parseMultiCostRow(relItemId, rowBlock, includeMeat)?.let { byRowId[rowId] = it }
        }

        return byRowId.values.toList()
    }

    /** Single-cost shop rows without ItemDatabase currency resolution (dynamic sync helpers). */
    fun parseSingleCostRows(html: String): List<ParsedSingleCostRow> =
        SINGLE_COST_PATTERN.findAll(html).mapNotNull { match ->
            ParsedSingleCostRow(
                rowId = match.groupValues[6].toIntOrNull() ?: return@mapNotNull null,
                itemId = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null,
                itemName = match.groupValues[3].trim(),
                currencyName = match.groupValues[4].trim(),
                price = match.groupValues[5].replace(",", "").toIntOrNull() ?: return@mapNotNull null,
            )
        }.toList()

    private fun parseSingleCostRow(
        itemId: Int,
        currencyName: String,
        price: Int,
        rowId: Int,
    ): ShopRow? {
        val costs = if (currencyName.equals("Meat", ignoreCase = true)) {
            listOf(ItemStack(itemId = -1, count = price, isMeat = true))
        } else {
            val currencyItem = ItemDatabase.getByName(currencyName) ?: return null
            listOf(ItemStack(itemId = currencyItem.id, count = price))
        }
        return ShopRow(
            rowId = rowId,
            item = ItemStack(itemId = itemId, count = 1),
            costs = costs,
        )
    }

    private fun parseMultiCostRow(
        relItemId: Int,
        rowBlock: String,
        includeMeat: Boolean,
    ): ShopRow? {
        var rowId = 0
        var itemId = relItemId
        var itemCount = 1
        var descId: String? = null
        val costs = mutableListOf<ItemStack>()
        var tds = 0
        var even = true
        var isMeat = false
        var skip = false
        var iDescId: String? = null

        for (td in TD_PATTERN.findAll(rowBlock)) {
            val attrs = td.groupValues[1]
            val text = td.groupValues[2]
            when (++tds) {
                1 -> continue
                2 -> {
                    val m2 = TD2_PATTERN.find(text) ?: return null
                    descId = m2.groupValues[2].ifBlank { null }
                }
                3 -> {
                    if (descId == null) return null
                    val names = TD3_PATTERN.findAll(text).map { it.groupValues[1].trim() }.toList()
                    if (names.size >= 2) {
                        itemCount = names[1].toIntOrNull() ?: 1
                    }
                    val resolved = ItemDatabase.getByDescId(descId)?.id
                    if (resolved != null && resolved > 0) itemId = resolved
                }
                else -> {
                    if (text.contains("shop.php", ignoreCase = true)) {
                        IROW_PATTERN.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { rowId = it }
                        continue
                    }
                    if (attrs.contains("class=tiny", ignoreCase = true)) {
                        even = true
                        continue
                    }
                    if (even) {
                        even = false
                        isMeat = text.contains("meat.gif", ignoreCase = true)
                        if (!isMeat) {
                            iDescId = TD2_PATTERN.find(text)?.groupValues?.get(2)?.ifBlank { null }
                        }
                    } else {
                        even = true
                        if (isMeat && !includeMeat) {
                            skip = true
                            continue
                        }
                        val count = IODD_PATTERN.find(text)?.groupValues?.get(1)
                            ?.replace(",", "")
                            ?.toIntOrNull() ?: 1
                        if (isMeat) {
                            costs.add(ItemStack(itemId = -1, count = count, isMeat = true))
                        } else if (iDescId != null) {
                            val costId = ItemDatabase.getByDescId(iDescId)?.id ?: continue
                            costs.add(ItemStack(itemId = costId, count = count))
                        }
                    }
                }
            }
        }

        if (skip || costs.isEmpty() || rowId <= 0) return null
        return ShopRow(
            rowId = rowId,
            item = ItemStack(itemId = itemId, count = itemCount),
            costs = costs,
        )
    }
}
