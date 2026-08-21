package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Conduct the Band choice 1526.
 */
object AprilBandChoiceSync {

    const val CHOICE_ID = 1526

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        turnsPlayed: Int = 0,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        return when (decision) {
            in 1..3 -> {
                preferences.setInt("nextAprilBandTurn", turnsPlayed + 11)
                true
            }
            in 4..8 -> {
                val next = (preferences.getInt("_aprilBandInstruments", 0) + 1).coerceAtMost(2)
                preferences.setInt("_aprilBandInstruments", next)
                true
            }
            else -> false
        }
    }
}
