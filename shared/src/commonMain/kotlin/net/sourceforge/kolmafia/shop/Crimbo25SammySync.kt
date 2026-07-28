package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [Crimbo25SammyRequest.visitShopRows] dynamic wad-cost refresh for crimbo25_sammy. */
object Crimbo25SammySync {

    const val SHOP_ID = CoinmasterVisitInventory.CRIMBO25_SAMMY
    const val CRYMBOCURRENCY = 12121
    const val COLD_WAD = 1452
    const val TWINKLY_WAD = 1450

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
        val rows = ShopRowParser.parseSingleCostRows(html).mapNotNull { parsed ->
            val item = parseItemStack(parsed.itemId, parsed.itemName) ?: return@mapNotNull null
            val costStack = parseCurrencyStack(parsed.currencyName, parsed.price) ?: return@mapNotNull null
            ShopRow(rowId = parsed.rowId, item = item, costs = listOf(costStack))
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
