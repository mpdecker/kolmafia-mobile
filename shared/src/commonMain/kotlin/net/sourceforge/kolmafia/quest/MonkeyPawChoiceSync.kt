package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.MonkeyPawRequest

/**
 * Thin adapter wiring [MonkeyPawRequest] visit/post into the choice pipeline (choice 1501).
 */
object MonkeyPawChoiceSync {

    const val CHOICE_ID = MonkeyPawRequest.CHOICE_ID

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        MonkeyPawRequest.visitChoice(html, preferences)
        return true
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains("Wish granted.")) return false
        MonkeyPawRequest.postChoice(html, preferences)
        return true
    }
}
