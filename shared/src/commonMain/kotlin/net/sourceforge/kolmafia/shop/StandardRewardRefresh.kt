package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop `(armoryandleggery)` listener — re-derive pulverization and rebuild armory buy rows. */
object StandardRewardRefresh {

    fun refreshArmoryRows(prefs: Preferences? = null, resetPref: Boolean = false) {
        StandardRewardDatabase.derivePulverization()
        ArmoryAndLeggeryShopRows.rebuild()
        if (resetPref && prefs != null) {
            prefs.setInt(StandardRewardCurrencySync.REFRESH_PREF, 0)
        }
    }
}
