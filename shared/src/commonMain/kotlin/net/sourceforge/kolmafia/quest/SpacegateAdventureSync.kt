package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.parseSpacegateAdventure] portable-gate hazard fill.
 */
object SpacegateAdventureSync {

    const val SPACEGATE = 494

    fun applyFromAdventure(
        url: String?,
        html: String,
        preferences: Preferences?,
        adventureId: String? = null,
    ): Boolean {
        if (preferences == null) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        if (area != SPACEGATE) return false
        if (preferences.getString("_spacegateHazards", "").isNotEmpty()) return false

        val hazards = mutableListOf<String>()
        val gear = mutableListOf<String>()
        for (h in SpacegateTerminalSync.HAZARDS) {
            if (html.contains(h.adventure)) {
                hazards.add(h.terminal)
                gear.add(h.gear)
            }
        }
        preferences.setString("_spacegateHazards", hazards.joinToString("|"))
        preferences.setString("_spacegateGear", gear.joinToString("|"))
        return true
    }
}
