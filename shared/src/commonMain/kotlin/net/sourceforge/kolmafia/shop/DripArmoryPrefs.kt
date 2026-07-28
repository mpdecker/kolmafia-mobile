package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop DripArmoryRequest visit sync + canBuyItem gates shared by sync + validate. */
object DripArmoryPrefs {

    const val DRIPPY_SHIELD = 10452
    const val SHIELD_UNLOCK_PREF = "drippyShieldUnlocked"

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        if (html.contains("drippy shield", ignoreCase = true)) {
            prefs.setBoolean(SHIELD_UNLOCK_PREF, true)
        }
    }

    fun isItemAvailable(
        itemId: Int,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (itemId != DRIPPY_SHIELD) return true
        return prefs?.getBoolean(SHIELD_UNLOCK_PREF, false) == true &&
            accessibleCount(DRIPPY_SHIELD) <= 0
    }
}
