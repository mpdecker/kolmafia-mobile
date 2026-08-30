package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.LeprecondoManager

/**
 * Desktop [LeprecondoManager.visit] for choice 1556 (Phases 3366–3380).
 */
object LeprecondoChoiceSync {

    const val CHOICE_ID = 1556

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        LeprecondoManager.visit(html, preferences)
        return true
    }
}
