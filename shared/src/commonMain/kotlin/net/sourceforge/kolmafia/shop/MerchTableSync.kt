package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [MerchTableRequest.visitShop] dynamic row refresh for conmerch. */
object MerchTableSync {

    const val SHOP_ID = CoinmasterVisitInventory.CONMERCH
    const val TWITCHING_TELEVISION_TATTOO = 9148
    const val AVAILABLE_MR_A_PREF = "availableMerchMrA"
    const val AVAILABLE_CHRONERS_PREF = "availableMerchChroners"

    private const val MR_ACCESSORY = 194
    private const val CHRONER = 7567

    private val MR_A_TOKEN_PATTERN =
        Regex("""You have ([\w,]+) Mr\. Accessor(?:y|ies) to trade\.""", RegexOption.IGNORE_CASE)
    private val CHRONER_TOKEN_PATTERN =
        Regex("""You have ([\w,]+) Mr\. Chroner to trade\.""", RegexOption.IGNORE_CASE)

    private val ITEM_PATTERN = Regex(
        """<tr rel="(\d+)">.*?onClick='javascript:descitem\((\d+)\)'>.*?<b>(.*?)</b>.*?title="(.*?)".*?<b>([\d,]+)</b>.*?whichrow=(\d+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        TimeTowerSync.syncFromChronerShopHtml(html, prefs)
        syncTokenBalances(html, prefs)

        val rows = mutableListOf<ShopRow>()
        for (match in ITEM_PATTERN.findAll(html)) {
            val itemId = match.groupValues[1].toIntOrNull() ?: continue
            val currencyName = match.groupValues[4].trim()
            val price = match.groupValues[5].replace(",", "").toIntOrNull() ?: continue
            val rowId = match.groupValues[6].toIntOrNull() ?: continue

            val currencyId = when {
                currencyName.equals("Chroner", ignoreCase = true) -> CHRONER
                else -> MR_ACCESSORY
            }

            rows.add(
                ShopRow(
                    rowId = rowId,
                    item = ItemStack(itemId = itemId, count = 1),
                    costs = listOf(ItemStack(itemId = currencyId, count = price)),
                ),
            )
        }

        CoinmasterVisitInventory.replaceBuyRows(SHOP_ID, rows)
    }

    private fun syncTokenBalances(html: String, prefs: Preferences) {
        MR_A_TOKEN_PATTERN.find(html)?.let { match ->
            parseTokenCount(match.groupValues[1])?.let { prefs.setInt(AVAILABLE_MR_A_PREF, it) }
        }
        CHRONER_TOKEN_PATTERN.find(html)?.let { match ->
            parseTokenCount(match.groupValues[1])?.let { prefs.setInt(AVAILABLE_CHRONERS_PREF, it) }
        }
    }

    private fun parseTokenCount(raw: String): Int? =
        raw.replace(",", "").toIntOrNull()
}
