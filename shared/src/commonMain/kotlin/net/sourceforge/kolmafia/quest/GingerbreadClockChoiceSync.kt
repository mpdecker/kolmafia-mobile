package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Setting the Clock choice 1215.
 */
object GingerbreadClockChoiceSync {

    const val CHOICE_ID = 1215

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setBoolean("_gingerbreadClockVisited", true)
        if (decision == 1) {
            preferences.setBoolean("_gingerbreadClockAdvanced", true)
            preferences.setInt(
                "_gingerbreadCityTurns",
                preferences.getInt("_gingerbreadCityTurns", 0) + 1,
            )
        }
        return true
    }
}
