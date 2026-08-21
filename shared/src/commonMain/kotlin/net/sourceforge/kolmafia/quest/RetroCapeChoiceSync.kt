package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] retro cape setup choices 1437 / 1438.
 * Defers ItemDatabase.setCapeSkills().
 */
object RetroCapeChoiceSync {

    const val WASH_CHOICE = 1437
    const val HERO_CHOICE = 1438

    val CHOICE_IDS = setOf(WASH_CHOICE, HERO_CHOICE)

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            WASH_CHOICE -> {
                val instructions = when (decision) {
                    2 -> "hold"
                    3 -> "thrill"
                    4 -> "kiss"
                    5 -> "kill"
                    else -> return false
                }
                preferences.setString("retroCapeWashingInstructions", instructions)
                true
            }
            HERO_CHOICE -> {
                val hero = when (decision) {
                    1 -> "vampire"
                    2 -> "heck"
                    3 -> "robot"
                    else -> return false
                }
                preferences.setString("retroCapeSuperhero", hero)
                true
            }
            else -> false
        }
    }
}
