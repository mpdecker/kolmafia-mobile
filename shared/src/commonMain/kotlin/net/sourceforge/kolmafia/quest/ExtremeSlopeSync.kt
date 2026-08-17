package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleExtremityChange] + [QuestManager.handleMcLargehugeChange].
 */
object ExtremeSlopeSync {

    const val EXTREME_SLOPE = 273

    fun applyFromAdventure(
        adventureId: String?,
        html: String,
        preferences: Preferences?,
        url: String? = null,
    ): Boolean {
        if (preferences == null) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        if (area != EXTREME_SLOPE) return false
        if (html.contains("Discovering Your Extremity") ||
            html.contains("2 eXXtreme 4 U") ||
            html.contains("3 eXXXtreme 4ever 6pack")
        ) {
            preferences.setInt(
                "currentExtremity",
                preferences.getInt("currentExtremity", 0) + 1,
            )
            return true
        }
        return false
    }

    fun applyCloudyPeak(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        val location = url.orEmpty()
        if (!location.contains("whichplace=mclargehuge", ignoreCase = true) &&
            !location.contains("cloudypeak", ignoreCase = true)
        ) {
            return false
        }
        if (html.contains("you spy a crude stone staircase") ||
            html.contains("notice a set of crude carved stairs")
        ) {
            questDatabase.setQuestIfBetter(Quest.TRAPPER, "step3")
            preferences.setInt("currentExtremity", 0)
            return true
        }
        return false
    }
}
