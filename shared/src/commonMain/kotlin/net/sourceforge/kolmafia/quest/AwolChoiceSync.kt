package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Go West, Young Adventurer! choice 1176 —
 * Snake Oiler start sets awolMedicine/awolVenom.
 */
object AwolChoiceSync {

    const val CHOICE_ID = 1176

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null || decision != 3) return false
        preferences.setInt("awolMedicine", 3)
        preferences.setInt("awolVenom", 3)
        return true
    }
}
