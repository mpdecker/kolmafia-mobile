package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop ResultProcessor armory-and-leggery listener when pulverized currency is acquired. */
object StandardRewardCurrencySync {

    const val REFRESH_PREF = "_armoryAndLeggeryCurrencyRefresh"

    fun onInventoryDelta(
        before: Map<Int, Int>,
        after: Map<Int, Int>,
        prefs: Preferences?,
    ) {
        if (prefs == null) return
        for ((itemId, qty) in after) {
            if (qty <= before[itemId] ?: 0) continue
            if (StandardRewardDatabase.isPulverizedStandardReward(itemId)) {
                prefs.setInt(REFRESH_PREF, prefs.getInt(REFRESH_PREF, 0) + 1)
                StandardRewardRefresh.refreshArmoryRows(prefs)
                return
            }
        }
    }
}
