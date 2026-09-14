package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop Island War camp coinmaster [accessible] gates
 * (HTTP Residual LI Track A: Dimemaster / Quartersmaster).
 */
object IslandWarShopAccessibility {

    const val WAR_HIPPY_HEADBAND = 2337
    const val WAR_HIPPY_CORDS = 2032
    const val WAR_HIPPY_GLASSES = 2033
    const val WAR_FRAT_HELMET = 2069
    const val WAR_FRAT_PANTS = 2070
    const val WAR_FRAT_PIN = 2353

    fun dimemasterInaccessible(
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): String? {
        if (prefs?.getString("warProgress", "unstarted") != "started") {
            return "You're not at war."
        }
        if (!hasWarHippyOutfit(accessibleCount)) {
            return "You don't have the War Hippy Fatigues"
        }
        return null
    }

    fun quartersmasterInaccessible(
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): String? {
        if (prefs?.getString("warProgress", "unstarted") != "started") {
            return "You're not at war."
        }
        if (!hasWarFratOutfit(accessibleCount)) {
            return "You don't have the Frat Warrior Fatigues"
        }
        return null
    }

    fun hasWarHippyOutfit(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(WAR_HIPPY_HEADBAND) > 0 &&
            accessibleCount(WAR_HIPPY_CORDS) > 0 &&
            accessibleCount(WAR_HIPPY_GLASSES) > 0

    fun hasWarFratOutfit(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(WAR_FRAT_HELMET) > 0 &&
            accessibleCount(WAR_FRAT_PANTS) > 0 &&
            accessibleCount(WAR_FRAT_PIN) > 0
}
