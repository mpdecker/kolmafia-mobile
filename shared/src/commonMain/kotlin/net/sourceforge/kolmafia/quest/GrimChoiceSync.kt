package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.GrimRequest

/**
 * Desktop [ChoiceControl] Barely Tales choice 835.
 */
object GrimChoiceSync {

    const val CHOICE_ID = GrimRequest.CHOICE_ID

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision == 0) return false
        preferences.setBoolean(GrimRequest.BUFF_USED_PREF, true)
        return true
    }
}
