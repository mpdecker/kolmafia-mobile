package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] So Cold choice 1418 —
 * prefs only (Melodramedary XP lose deferred).
 */
object EntauntaunedChoiceSync {

    const val CHOICE_ID = 1418

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        preferences.setBoolean("_entauntaunedToday", true)
        return true
    }
}
