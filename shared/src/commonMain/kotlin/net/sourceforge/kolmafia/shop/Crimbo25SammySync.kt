package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [Crimbo25SammyRequest.visitShopRows] dynamic wad-cost refresh for crimbo25_sammy. */
object Crimbo25SammySync {

    const val SHOP_ID = CoinmasterVisitInventory.CRIMBO25_SAMMY
    const val CRYMBOCURRENCY = 12121
    const val COLD_WAD = 1452
    const val TWINKLY_WAD = 1450

    private val ITEM_PATTERN = Regex(
        """<tr rel="(\d+)">.*?onClick='javascript:descitem\((\d+)\)'>.*?<b>(.*?)</b>.*?title="(.*?)".*?<b>([\d,]+)</b>.*?whichrow=(\d+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val PAREN_COUNT_PATTERN = Regex("""^(.+?)\s*\(([\d,]+)\)$""")

    private val CURRENCY_NAME_TO_ID = mapOf(
        "cold wad" to COLD_WAD,
        "twinkly wad" to TWINKLY_WAD,
        "burnt incisor" to 12116,
        "burnt phalange" to 12117,
        "burnt rib" to 12119,
        "burnt radius" to 12118,
        "burnt skull" to 12120,
        "skull of claus" to 12128,
        "smoldering bone dust" to 12135,
        "crymbocurrency" to CRYMBOCURRENCY,
    )

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        val rows = mutableListOf<ShopRow>()
        for (match in ITEM_PATTERN.findAll(html)) {
            val itemId = match.groupValues[1].toIntOrNull() ?: continue
            val itemName = match.groupValues[3].trim()
            val currencyName = match.groupValues[4].trim()
            val price = match.groupValues[5].replace(",", "").toIntOrNull() ?: continue
            val rowId = match.groupValues[6].toIntOrNull() ?: continue

            val item = parseItemStack(itemId, itemName) ?: continue
            val cost = parseCurrencyStack(currencyName, price) ?: continue

            rows.add(
                ShopRow(
                    rowId = rowId,
                    item = item,
                    costs = listOf(cost),
                ),
            )
        }

        CoinmasterVisitInventory.replaceBuyRows(SHOP_ID, rows)
    }

    private fun parseItemStack(itemId: Int, itemName: String): ItemStack? {
        if (itemName.startsWith("Crymbocurrency", ignoreCase = true)) {
            val count = parseParenCount(itemName)?.second ?: 1
            return ItemStack(CRYMBOCURRENCY, count)
        }
        return ItemStack(itemId, 1)
    }

    private fun parseCurrencyStack(currencyName: String, priceFromHtml: Int): ItemStack? {
        parseParenCount(currencyName)?.let { (baseName, count) ->
            return currencyId(baseName)?.let { ItemStack(it, count) }
        }
        return currencyId(currencyName)?.let { ItemStack(it, priceFromHtml) }
    }

    private fun parseParenCount(name: String): Pair<String, Int>? {
        val match = PAREN_COUNT_PATTERN.find(name.trim()) ?: return null
        val base = match.groupValues[1].trim()
        val count = match.groupValues[2].replace(",", "").toIntOrNull() ?: return null
        return base to count
    }

    private fun currencyId(name: String): Int? =
        CURRENCY_NAME_TO_ID.entries.firstOrNull { (key, _) ->
            name.equals(key, ignoreCase = true)
        }?.value
}
