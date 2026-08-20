package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Shining Mauve Backwards In Time choice 1119.
 */
object DmtChoiceSync {

    const val CHOICE_ID = 1119

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        ascensionNumber: Int,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setInt("encountersUntilDMTChoice", 49)
        if (decision == 4 && preferences.getInt("lastDMTDuplication", -1) != ascensionNumber) {
            preferences.setInt("lastDMTDuplication", ascensionNumber)
        }
        return true
    }
}
