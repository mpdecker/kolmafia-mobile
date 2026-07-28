package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [FlowerTradeinRequest.visitShopRows] dynamic row refresh for flowertradein. */
object FlowerTradeinSync {

    const val SHOP_ID = CoinmasterVisitInventory.FLOWER_TRADEIN
    const val CHRONER = 7567

    private val PAREN_COUNT_PATTERN = Regex("""^(.+?)\s*\(([\d,]+)\)$""")

    private val FLOWER_NAME_TO_ID = mapOf(
        "rose" to FlowerTradeinAccessibility.ROSE,
        "red tulip" to FlowerTradeinAccessibility.RED_TULIP,
        "white tulip" to FlowerTradeinAccessibility.WHITE_TULIP,
        "blue tulip" to FlowerTradeinAccessibility.BLUE_TULIP,
    )

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        val rows = ShopRowParser.parseSingleCostRows(html).mapNotNull { parsed ->
            mapRow(parsed)
        }
        CoinmasterVisitInventory.replaceBuyRows(SHOP_ID, rows)
    }

    internal fun mapRow(parsed: ShopRowParser.ParsedSingleCostRow): ShopRow? {
        val item = parseChronerItem(parsed.itemId, parsed.itemName) ?: return null
        val flowerId = flowerId(parsed.currencyName) ?: return null
        return ShopRow(
            rowId = parsed.rowId,
            item = item,
            costs = listOf(ItemStack(itemId = flowerId, count = parsed.price)),
        )
    }

    private fun parseChronerItem(itemId: Int, itemName: String): ItemStack? {
        if (!itemName.startsWith("Chroner", ignoreCase = true)) {
            return ItemStack(itemId, 1)
        }
        val count = parseParenCount(itemName)?.second ?: 1
        return ItemStack(CHRONER, count)
    }

    private fun flowerId(currencyName: String): Int? =
        FLOWER_NAME_TO_ID.entries.firstOrNull { (name, _) ->
            currencyName.equals(name, ignoreCase = true)
        }?.value

    private fun parseParenCount(name: String): Pair<String, Int>? {
        val match = PAREN_COUNT_PATTERN.find(name.trim()) ?: return null
        val base = match.groupValues[1].trim()
        val count = match.groupValues[2].replace(",", "").toIntOrNull() ?: return null
        return base to count
    }
}
