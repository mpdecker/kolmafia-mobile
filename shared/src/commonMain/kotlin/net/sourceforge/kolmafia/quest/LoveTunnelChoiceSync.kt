package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] The Tunnel of L.O.V.E. choice 1222.
 */
object LoveTunnelChoiceSync {

    const val CHOICE_ID = 1222

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        preferences.setBoolean("_loveTunnelUsed", true)
        return true
    }
}
