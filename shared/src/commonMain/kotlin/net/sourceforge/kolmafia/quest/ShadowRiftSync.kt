package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [FightRequest] SHADOW_RIFT `_shadowRiftCombats` pref increment. */
object ShadowRiftSync {

    const val SHADOW_RIFT_COMBATS_PREF = "_shadowRiftCombats"

    fun incrementCombats(preferences: Preferences?) {
        val prefs = preferences ?: return
        prefs.setInt(SHADOW_RIFT_COMBATS_PREF, prefs.getInt(SHADOW_RIFT_COMBATS_PREF, 0) + 1)
    }

    fun isShadowRiftLocation(location: String): Boolean =
        location.contains("Shadow Rift", ignoreCase = true)
}
