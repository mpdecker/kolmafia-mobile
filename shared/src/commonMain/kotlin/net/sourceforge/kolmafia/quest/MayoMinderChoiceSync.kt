package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Mayo Minder™ choice 1076.
 */
object MayoMinderChoiceSync {

    const val CHOICE_ID = 1076

    fun apply(choiceId: Int, decision: Int, preferences: Preferences?): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val setting = when (decision) {
            1 -> "Mayonex"
            2 -> "Mayodiol"
            3 -> "Mayostat"
            4 -> "Mayozapine"
            5 -> "Mayoflex"
            6 -> ""
            else -> return false
        }
        preferences.setString("mayoMinderSetting", setting)
        return true
    }
}
