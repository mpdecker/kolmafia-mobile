package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] The Council of Loathing choice 1565 —
 * scoped king liberation (pref + character flag only).
 */
object CouncilChoiceSync {

    const val CHOICE_ID = 1565

    private const val FREE_RALPH =
        "You free King Ralph, signalling a triumphant end to your submaritime adventure"

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        setKingLiberated: () -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains(FREE_RALPH)) return false
        preferences.setBoolean("kingLiberated", true)
        setKingLiberated()
        return true
    }
}
