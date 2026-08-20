package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Spoopy choice 1110.
 */
object SpoopyChoiceSync {

    const val CHOICE_ID = 1110

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 5) return false
        when {
            html.contains("You board up the doghouse") ->
                preferences.setBoolean("doghouseBoarded", true)
            html.contains("You unboard-up the doghouse") ->
                preferences.setBoolean("doghouseBoarded", false)
            else -> return false
        }
        return true
    }

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setBoolean("doghouseBoarded", !html.contains("Board up the doghouse"))
        return true
    }
}
