package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleFarmChange] duck-area clear CSV.
 */
object FarmDuckSync {

    val FARM_AREAS = setOf(141, 142, 143, 144, 145, 146, 147)

    fun applyFromAdventure(
        adventureId: String?,
        html: String,
        preferences: Preferences?,
        url: String? = null,
    ): Boolean {
        if (preferences == null) return false
        if (!html.contains("There are no more ducks here")) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        if (area !in FARM_AREAS) return false
        val id = area.toString()
        val current = preferences.getString("duckAreasCleared", "")
        val parts = if (current.isBlank()) emptyList() else current.split(',').map { it.trim() }
        if (id in parts) return false
        preferences.setString(
            "duckAreasCleared",
            if (current.isBlank()) id else "$current,$id",
        )
        return true
    }
}
