package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.NumberologyRequest

/**
 * Desktop [ChoiceControl] Doing the Maths choice 1103.
 */
object NumberologyChoiceSync {

    const val CHOICE_ID = NumberologyRequest.CHOICE_ID

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (html.contains("Try again")) return false
        preferences.setInt(
            NumberologyRequest.PREF_UNIVERSE_CALCULATED,
            preferences.getInt(NumberologyRequest.PREF_UNIVERSE_CALCULATED, 0) + 1,
        )
        return true
    }
}
