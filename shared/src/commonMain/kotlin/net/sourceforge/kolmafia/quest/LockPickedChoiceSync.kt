package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Lock Picking choice 1414.
 */
object LockPickedChoiceSync {

    const val CHOICE_ID = 1414

    fun apply(
        choiceId: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setBoolean("lockPicked", true)
        return true
    }
}
