package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [FightRequest] New You saw sharpening combat writers.
 */
object NewYouCombatSync {

    private val SHARPEN_SAW_PATTERN = Regex(
        """You're really sharpening the old saw\.\s+Looks like you've done (\d+) out of (\d+)!""",
    )

    fun apply(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null || html.isBlank()) return false
        if (html.contains("You're really sharpening the old saw.")) {
            val match = SHARPEN_SAW_PATTERN.find(html) ?: return false
            preferences.setString("_newYouQuestSharpensDone", match.groupValues[1])
            preferences.setString("_newYouQuestSharpensToDo", match.groupValues[2])
            return true
        }
        if (html.contains("Your saw is so sharp!")) {
            preferences.setString("_newYouQuestMonster", "")
            preferences.setString("_newYouQuestSkill", "")
            preferences.setInt("_newYouQuestSharpensDone", 0)
            preferences.setInt("_newYouQuestSharpensToDo", 0)
            preferences.setBoolean("_newYouQuestCompleted", true)
            questDatabase?.setProgress(Quest.NEW_YOU, QuestDatabase.UNSTARTED)
            return true
        }
        return false
    }
}
