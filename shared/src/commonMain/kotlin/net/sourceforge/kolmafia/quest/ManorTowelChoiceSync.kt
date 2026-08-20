package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Off the Rack choice 882.
 */
object ManorTowelChoiceSync {

    const val CHOICE_ID = 882

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        ascensionNumber: Int,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        if (!html.contains("You never know when it might come in handy.")) return false
        preferences.setInt("lastTowelAscension", ascensionNumber)
        return true
    }
}
