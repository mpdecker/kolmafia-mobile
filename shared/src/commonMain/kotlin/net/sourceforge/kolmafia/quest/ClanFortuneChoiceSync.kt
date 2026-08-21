package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClanFortuneRequest

/**
 * Thin adapter wiring [ClanFortuneRequest.parseResponse] into choice 1278.
 */
object ClanFortuneChoiceSync {

    const val CHOICE_ID = ClanFortuneRequest.CHOICE_ID

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val before = preferences.getBoolean(ClanFortuneRequest.BUFF_USED_PREF, false)
        ClanFortuneRequest.parseResponse(choiceUrl.ifBlank { "choice.php" }, html, preferences)
        return preferences.getBoolean(ClanFortuneRequest.BUFF_USED_PREF, false) != before ||
            html.contains("Relationship Fortune Teller")
    }
}
