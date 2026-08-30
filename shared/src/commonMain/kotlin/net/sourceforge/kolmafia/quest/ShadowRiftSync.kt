package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.adventure.ShadowRift
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [FightRequest] SHADOW_RIFT `_shadowRiftCombats` + ingress pref write. */
object ShadowRiftSync {

    const val SHADOW_RIFT_COMBATS_PREF = "_shadowRiftCombats"
    const val INGRESS_PREF = ShadowRift.INGRESS_PREF

    fun incrementCombats(preferences: Preferences?) {
        val prefs = preferences ?: return
        prefs.setInt(SHADOW_RIFT_COMBATS_PREF, prefs.getInt(SHADOW_RIFT_COMBATS_PREF, 0) + 1)
    }

    fun isShadowRiftLocation(location: String): Boolean =
        location.contains("Shadow Rift", ignoreCase = true)

    /**
     * Desktop [KoLAdventure.findAdventure] shadow_rift place.php enter —
     * writes [INGRESS_PREF] from whichplace=.
     */
    fun applyIngressFromUrl(url: String?, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        val urlString = url.orEmpty()
        if (!urlString.contains("place.php", ignoreCase = true)) return false
        val place = Regex("""whichplace=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(urlString)?.groupValues?.getOrNull(1) ?: return false
        val rift = ShadowRift.findPlace(place) ?: return false
        prefs.setString(INGRESS_PREF, rift.place)
        return true
    }
}