package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleElVibratoChange] + [CampgroundRequest.updateElVibratoPortal].
 */
object ElVibratoSync {

    const val EL_VIBRATO_ISLAND = 164
    const val TRAPEZOID = 3198

    fun applyFromAdventure(
        adventureId: String?,
        preferences: Preferences?,
        url: String? = null,
    ): Boolean {
        if (preferences == null) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        if (area != EL_VIBRATO_ISLAND) return false
        val energy = (preferences.getInt("currentPortalEnergy", 0) - 1).coerceAtLeast(0)
        preferences.setInt("currentPortalEnergy", energy)
        updatePortalTrapezoid(preferences)
        return true
    }

    fun applyFromCampground(html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        var changed = false
        if (html.contains("portal1.gif")) {
            var charges = preferences.getInt("currentPortalEnergy", 0)
            if (charges == 0) {
                preferences.setInt("currentPortalEnergy", 20)
                charges = 20
            }
            updatePortalTrapezoid(preferences)
            changed = true
        }
        if (html.contains("portal2.gif")) {
            preferences.setInt("currentPortalEnergy", 0)
            updatePortalTrapezoid(preferences)
            changed = true
        }
        return changed
    }

    fun updatePortalTrapezoid(preferences: Preferences) {
        val charges = preferences.getInt("currentPortalEnergy", 0)
        CampgroundInventorySync.setItem(preferences, TRAPEZOID, charges)
    }
}
