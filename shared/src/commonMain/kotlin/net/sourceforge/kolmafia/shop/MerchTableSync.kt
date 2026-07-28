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

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        TimeTowerSync.syncFromChronerShopHtml(html, prefs)
        syncTokenBalances(html, prefs)

        val rows = ShopRowParser.parseSingleCostRows(html).map { parsed ->
            val currencyId = when {
                parsed.currencyName.equals("Chroner", ignoreCase = true) -> CHRONER
                else -> MR_ACCESSORY
            }
            ShopRow(
                rowId = parsed.rowId,
                item = ItemStack(itemId = parsed.itemId, count = 1),
                costs = listOf(ItemStack(itemId = currencyId, count = parsed.price)),
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
