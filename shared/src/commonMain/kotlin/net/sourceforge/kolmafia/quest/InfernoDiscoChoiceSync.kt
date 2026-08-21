package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] The Towering Inferno Discotheque choice 1090.
 */
object InfernoDiscoChoiceSync {

    const val CHOICE_ID = 1090

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision <= 1) return false
        preferences.setBoolean("_infernoDiscoVisited", true)
        return true
    }
}
