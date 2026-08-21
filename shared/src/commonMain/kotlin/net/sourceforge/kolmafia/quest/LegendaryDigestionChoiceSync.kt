package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Legendary Digestion choice 1599 —
 * post organ/pref writers (visit UseItem consume deferred).
 */
object LegendaryDigestionChoiceSync {

    const val CHOICE_ID = 1599

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        adjustFullness: (Int) -> Unit = {},
        adjustSpleen: (Int) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        return when (decision) {
            1 -> {
                preferences.setBoolean("_legendaryNoodlesSpleen", true)
                adjustFullness(-1)
                adjustSpleen(1)
                true
            }
            2 -> {
                preferences.setInt(
                    "legendaryNoodlesAmygdala",
                    preferences.getInt("legendaryNoodlesAmygdala", 0) + 5,
                )
                true
            }
            3 -> {
                preferences.setInt(
                    "legendaryNoodlesSkin",
                    preferences.getInt("legendaryNoodlesSkin", 0) + 5,
                )
                true
            }
            4 -> true // familiar +50 non-combat XP deferred (no simple API)
            5 -> {
                preferences.setInt(
                    "legendaryNoodlesStomach",
                    preferences.getInt("legendaryNoodlesStomach", 0) + 3,
                )
                true
            }
            else -> false
        }
    }
}
