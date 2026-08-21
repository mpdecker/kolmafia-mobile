package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop Barely Tales choice 835; its sole residual mutation is the Grim buff flag. */
object BarelyTalesChoiceSync {
    const val CHOICE_ID = 835
    fun apply(choiceId: Int, decision: Int, preferences: Preferences?): Boolean {
        if (choiceId != CHOICE_ID || decision == 0 || preferences == null) return false
        preferences.setBoolean("_grimBuff", true)
        return true
    }
}
