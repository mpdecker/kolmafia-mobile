package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Strange Stalagmite(s) choice 1491 —
 * visit marks `_strangeStalagmiteUsed`.
 */
object StalagmiteChoiceSync {

    const val CHOICE_ID = 1491

    const val USED_PREF = "_strangeStalagmiteUsed"

    fun applyVisit(
        choiceId: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setBoolean(USED_PREF, true)
        return true
    }
}
