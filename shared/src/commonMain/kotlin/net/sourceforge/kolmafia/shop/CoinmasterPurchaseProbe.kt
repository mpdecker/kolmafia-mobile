package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop CoinMasterPurchaseRequest.availableItem / affordableCount (meat/token affordability). */
object CoinmasterPurchaseProbe {

    fun canPurchaseIgnoringMeat(
        itemId: Int,
        state: CharacterState,
        prefs: Preferences? = null,
        accessibleCount: (Int) -> Int = { 0 },
    ): Boolean {
        if (prefs?.getBoolean("autoSatisfyWithCoinmasters", false) != true) return false
        val (master, row) = CoinmasterDatabase.findBuyRowForItem(itemId) ?: return false
        if (!CoinmasterAccessibility.isAccessible(master, state)) return false
        return affordableCount(row, state, accessibleCount) > 0
    }

    internal fun affordableCount(
        row: ShopRow,
        state: CharacterState,
        accessibleCount: (Int) -> Int,
    ): Int {
        if (row.costs.isEmpty()) {
            if (row.price <= 0) return 0
            return if (state.meat >= row.price) 1 else 0
        }
        var minAffordable = Int.MAX_VALUE
        for (cost in row.costs) {
            if (cost.isMeat) {
                val affordable = if (cost.count <= 0) 0 else state.meat / cost.count
                minAffordable = minOf(minAffordable, affordable)
            } else {
                val tokens = accessibleCount(cost.itemId)
                val affordable = if (cost.count <= 0) 0 else tokens / cost.count
                minAffordable = minOf(minAffordable, affordable)
            }
        }
        return if (minAffordable == Int.MAX_VALUE) 0 else minAffordable
    }
}
