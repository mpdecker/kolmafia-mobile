package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [SpinMasterLatheRequest.visitShop] pref sync. */
object SpinMasterLatheSync {

    const val VISITED_PREF = "_spinmasterLatheVisited"

    fun syncFromShopHtml(prefs: Preferences) {
        prefs.setBoolean(VISITED_PREF, true)
    }
}
