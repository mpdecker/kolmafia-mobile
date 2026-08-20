package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] preChoice for Blech House choice 1345 —
 * resets `smutOrcNoncombatProgress` to 0.
 */
object BlechHouseChoiceSync {

    const val CHOICE_ID = 1345

    fun apply(
        choiceId: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setInt(SmutOrcCombatSync.PREF, 0)
        return true
    }
}
