package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Pick a Part choice 597.
 */
object GnomePartChoiceSync {

    const val CHOICE_ID = 597

    fun apply(
        choiceId: Int,
        preferences: Preferences?,
        refreshConcoctions: () -> Unit = { ConcoctionDatabase.refreshConcoctionsNow() },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setBoolean("_gnomePart", true)
        refreshConcoctions()
        return true
    }
}
