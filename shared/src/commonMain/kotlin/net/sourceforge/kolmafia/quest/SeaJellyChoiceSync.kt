package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Approach the Jellyfish choice 1219.
 */
object SeaJellyChoiceSync {

    const val CHOICE_ID = 1219

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        preferences.setBoolean("_seaJellyHarvested", true)
        return true
    }
}
