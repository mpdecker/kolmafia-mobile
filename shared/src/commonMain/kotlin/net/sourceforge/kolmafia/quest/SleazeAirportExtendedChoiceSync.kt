package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Sleaze Airport choices 918–920
 * (Yachtzee / Break Time / Eraser).
 */
object SleazeAirportExtendedChoiceSync {

    const val YACHTZEE = 918
    const val BREAK_TIME = 919
    const val ERASER = 920

    const val MOIST_BEADS = 7475
    const val MIND_DESTROYER = 7476

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        itemCount: (Int) -> Int = { 0 },
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        return when (choiceId) {
            YACHTZEE -> applyYachtzee(decision, html, itemCount, consumeItem)
            BREAK_TIME -> applyBreakTime(decision, html, preferences)
            ERASER -> applyEraser(decision, questDatabase, consumeItem)
            else -> false
        }
    }

    private fun applyYachtzee(
        decision: Int,
        html: String,
        itemCount: (Int) -> Int,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        if (decision != 3 || !html.contains("You open the captain's door")) return false
        val beads = minOf(itemCount(MOIST_BEADS), 100)
        if (beads > 0) consumeItem(MOIST_BEADS, beads)
        return true
    }

    private fun applyBreakTime(
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (decision != 1 || preferences == null) return false
        if (html.contains("You've already thoroughly")) {
            preferences.setInt("_sloppyDinerBeachBucks", 4)
        } else {
            preferences.setInt(
                "_sloppyDinerBeachBucks",
                preferences.getInt("_sloppyDinerBeachBucks", 0) + 1,
            )
        }
        return true
    }

    private fun applyEraser(
        decision: Int,
        questDatabase: QuestDatabase?,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        if (questDatabase == null) return false
        when (decision) {
            1 -> {
                questDatabase.setProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.UNSTARTED)
                questDatabase.setProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.UNSTARTED)
                questDatabase.setProgress(Quest.JIMMY_SALT, QuestDatabase.UNSTARTED)
            }
            2 -> {
                questDatabase.setProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.UNSTARTED)
                questDatabase.setProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.UNSTARTED)
                questDatabase.setProgress(Quest.TACO_DAN_FISH, QuestDatabase.UNSTARTED)
            }
            3 -> {
                questDatabase.setProgress(Quest.BRODEN_BACTERIA, QuestDatabase.UNSTARTED)
                questDatabase.setProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.UNSTARTED)
                questDatabase.setProgress(Quest.BRODEN_DEBT, QuestDatabase.UNSTARTED)
            }
            else -> return false
        }
        if (decision != 4) {
            consumeItem(MIND_DESTROYER, 1)
        }
        return true
    }
}
