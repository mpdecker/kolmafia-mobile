package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Peering Through Your Peridot choice 1557 visit —
 * merge last adventure id into `_perilLocations`.
 */
object PeridotChoiceSync {

    const val CHOICE_ID = 1557
    const val LOCATIONS_PREF = "_perilLocations"

    fun applyVisit(
        choiceId: Int,
        preferences: Preferences?,
        adventureId: String? = null,
        lastVisitedLocationName: String = "",
        resolveAdventureId: (String) -> String? = { name ->
            AdventureDatabase.getByName(name)?.snarfblat
        },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val id = adventureId?.takeIf { it.isNotBlank() }
            ?: resolveAdventureId(
                lastVisitedLocationName.ifBlank {
                    preferences.getString(Preferences.LAST_LOCATION, "")
                },
            )
            ?: return false
        val numericId = id.toIntOrNull() ?: return false
        val existing = preferences.getString(LOCATIONS_PREF, "")
            .split(',')
            .mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() }?.toIntOrNull() }
        val merged = (existing + numericId)
            .distinct()
            .sorted()
            .joinToString(",")
        preferences.setString(LOCATIONS_PREF, merged)
        return true
    }
}
