package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop pyramid chamber progress from upper/middle chamber adventures, applied post-fight /
 * post-adventure when snarfblat matches chamber IDs.
 */
object PyramidCombatSync {

    fun applyChamberProgress(
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        adventureId: String,
        responseText: String,
    ): Boolean {
        if (questDatabase == null || preferences == null || responseText.isBlank()) return false
        val area = adventureId.toIntOrNull()
            ?: Regex("""^(\d+)$""").find(adventureId)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return when (area) {
            PyramidVisitSync.UPPER_CHAMBER ->
                PyramidVisitSync.applyUpperChamber(responseText, questDatabase, preferences)
            PyramidVisitSync.MIDDLE_CHAMBER ->
                PyramidVisitSync.applyMiddleChamber(responseText, questDatabase, preferences)
            else -> false
        }
    }
}
