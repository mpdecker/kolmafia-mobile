package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Diggin' up a Gift! choice 1591 —
 * extract note → session log (no prefs).
 */
object DigGiftChoiceSync {

    const val CHOICE_ID = 1591

    private val DIGGING_GIFT = Regex(
        """Looks like they left a note: <div style="padding: 1em; margin: 1em; border: 1px solid black">(.*?)</div>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        sessionLog: (String) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        val note = DIGGING_GIFT.find(html)?.groupValues?.getOrNull(1)?.trim() ?: return false
        sessionLog("Note: $note")
        return true
    }
}
