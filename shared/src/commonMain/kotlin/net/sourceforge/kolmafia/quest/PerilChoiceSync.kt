package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Foreseeing Peril choice 1558.
 */
object PerilChoiceSync {

    const val CHOICE_ID = 1558
    const val MAX_PERILS = 3

    private val REMAINING = Regex(
        """You can foresee peril (\d+) more times? today""",
        RegexOption.IGNORE_CASE,
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (html.contains("You've already foreseen enough peril today.")) {
            preferences.setInt("_perilsForeseen", MAX_PERILS)
            return true
        }
        val remaining = REMAINING.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        preferences.setInt("_perilsForeseen", (MAX_PERILS - remaining).coerceIn(0, MAX_PERILS))
        return true
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (applyDecision(html, preferences)) return true
        // Visit-style remaining parse also runs on post responses.
        return applyVisit(choiceId, html, preferences)
    }

    fun applyDecision(
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (html.contains("You've already seen too much peril.")) {
            preferences.setInt("_perilsForeseen", MAX_PERILS)
            return true
        }
        if (html.contains("You gaze into your Peridot and foresee a horrible future")) {
            val next = (preferences.getInt("_perilsForeseen", 0) + 1).coerceAtMost(MAX_PERILS)
            preferences.setInt("_perilsForeseen", next)
            return true
        }
        return false
    }
}
