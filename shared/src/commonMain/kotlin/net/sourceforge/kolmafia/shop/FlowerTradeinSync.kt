package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [FlowerTradeinRequest.visitShopRows] dynamic row refresh for flowertradein. */
object FlowerTradeinSync {

    const val SHOP_ID = CoinmasterVisitInventory.FLOWER_TRADEIN
    const val CHRONER = 7567

    private val ITEM_PATTERN = Regex(
        """<tr rel="(\d+)">.*?onClick='javascript:descitem\((\d+)\)'>.*?<b>(.*?)</b>.*?title="(.*?)".*?<b>([\d,]+)</b>.*?whichrow=(\d+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val FLOWER_NAME_TO_ID = mapOf(
        "rose" to FlowerTradeinAccessibility.ROSE,
        "red tulip" to FlowerTradeinAccessibility.RED_TULIP,
        "white tulip" to FlowerTradeinAccessibility.WHITE_TULIP,
        "blue tulip" to FlowerTradeinAccessibility.BLUE_TULIP,
    )

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        val rows = mutableListOf<ShopRow>()
        for (match in ITEM_PATTERN.findAll(html)) {
            val itemName = match.groupValues[3].trim()
            val currencyName = match.groupValues[4].trim()
            val price = match.groupValues[5].replace(",", "").toIntOrNull() ?: continue
            val rowId = match.groupValues[6].toIntOrNull() ?: continue

            val boughtItemId = when {
                itemName.equals("Chroner", ignoreCase = true) -> CHRONER
                else -> match.groupValues[1].toIntOrNull() ?: continue
            }
            val flowerId = FLOWER_NAME_TO_ID.entries.firstOrNull { (name, _) ->
                currencyName.equals(name, ignoreCase = true)
            }?.value ?: continue

            rows.add(
                ShopRow(
                    rowId = rowId,
                    item = ItemStack(itemId = boughtItemId, count = 1),
                    costs = listOf(ItemStack(itemId = flowerId, count = price)),
                ),
            )
        }

        CoinmasterVisitInventory.replaceBuyRows(SHOP_ID, rows)
    }
}
