package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Caboose Distraction 1486 + Passenger Among Passengers 1487.
 */
object ElfGratitudeChoiceSync {

    const val CABOOSE_CHOICE = 1486
    const val PASSENGER_CHOICE = 1487

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            CABOOSE_CHOICE -> {
                if (decision != 2) return false
                preferences.setInt(
                    "elfGratitude",
                    preferences.getInt("elfGratitude", 0) + 3,
                )
                true
            }
            PASSENGER_CHOICE -> {
                preferences.setInt(
                    "elfGratitude",
                    preferences.getInt("elfGratitude", 0) + 5,
                )
                true
            }
            else -> false
        }
    }
}
