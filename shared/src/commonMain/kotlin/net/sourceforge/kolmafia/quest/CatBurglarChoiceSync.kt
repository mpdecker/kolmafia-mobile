package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] A Heist! choice 1320.
 */
object CatBurglarChoiceSync {

    const val CHOICE_ID = 1320
    const val HEISTS_PREF = "_catBurglarHeistsComplete"

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        preferences.setInt(HEISTS_PREF, preferences.getInt(HEISTS_PREF, 0) + 1)
        return true
    }
}
