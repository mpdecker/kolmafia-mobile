package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

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

    fun applyVisitShopRows(
        shopRows: List<ShopRow>,
        force: Boolean,
        sessionLogger: SessionLogger?,
    ) {
        val rows = shopRows.mapNotNull { mapShopRow(it) }
        if (rows.isNotEmpty()) {
            CoinmasterVisitInventory.replaceBuyRows(SHOP_ID, rows)
        }
    }

    fun applyVisitShop(
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) {
        if (prefs == null) return
        TimeTowerSync.syncFromChronerShopHtml(html, prefs)
        syncTokenBalances(html, prefs)
    }

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        applyVisitShop(html, url = null, prefs, sessionLogger = null, state = null)
        val rows = ShopRowParser.parseSingleCostRows(html).mapNotNull { mapParsedRow(it) }
        CoinmasterVisitInventory.replaceBuyRows(SHOP_ID, rows)
    }

    internal fun mapShopRow(row: ShopRow): ShopRow? {
        if (row.costs.size != 1) return null
        val cost = row.costs[0]
        val currencyId = currencyIdForCost(cost.itemId) ?: return null
        return ShopRow(
            rowId = row.rowId,
            item = row.item.copy(count = 1),
            costs = listOf(ItemStack(itemId = currencyId, count = cost.count)),
        )
    }

    internal fun mapParsedRow(parsed: ShopRowParser.ParsedSingleCostRow): ShopRow? {
        val currencyId = when {
            parsed.currencyName.equals("Chroner", ignoreCase = true) -> CHRONER
            else -> MR_ACCESSORY
        }
        return ShopRow(
            rowId = parsed.rowId,
            item = ItemStack(itemId = parsed.itemId, count = 1),
            costs = listOf(ItemStack(itemId = currencyId, count = parsed.price)),
        )
    }

    private fun currencyIdForCost(costItemId: Int): Int? {
        val name = ItemDatabase.getById(costItemId)?.name ?: return null
        return when {
            name.equals("Chroner", ignoreCase = true) -> CHRONER
            name.contains("Mr.", ignoreCase = true) && name.contains("Accessor", ignoreCase = true) -> MR_ACCESSORY
            else -> MR_ACCESSORY
        }
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
