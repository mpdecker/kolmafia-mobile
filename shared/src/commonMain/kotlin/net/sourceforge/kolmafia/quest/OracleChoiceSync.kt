package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] The Oracle choice 1190.
 */
object OracleChoiceSync {

    const val CHOICE_ID = 1190
    const val NO_SPOON = 9029

    private val TARGET_PATTERN =
        Regex("""don't remember leaving any spoons in (.*?)&quot;""")

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision == 2) {
            consumeItem(NO_SPOON, 1)
            questDatabase.setProgress(Quest.ORACLE, QuestDatabase.UNSTARTED)
            preferences.setInt(
                "sourceEnlightenment",
                preferences.getInt("sourceEnlightenment", 0) + 1,
            )
            preferences.setString("sourceOracleTarget", "")
            return true
        }
        if (decision > 3) return false
        questDatabase.setProgress(Quest.ORACLE, QuestDatabase.STARTED)
        TARGET_PATTERN.find(html)?.groupValues?.getOrNull(1)?.let { target ->
            preferences.setString("sourceOracleTarget", target)
        }
        return true
    }
}
