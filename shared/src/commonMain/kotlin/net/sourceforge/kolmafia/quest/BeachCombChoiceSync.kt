package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BeachManager
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [ChoiceControl] / [BeachManager] synchronization for choice 1388. */
object BeachCombChoiceSync {
    const val CHOICE_ID = 1388

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = false
        when (decision) {
            0 -> {
                changed = BeachManager.parseCombUsage(html, preferences) || changed
                changed = BeachManager.parseBeachMap(
                    html,
                    preferences,
                    sessionLogger?.let { logger -> { line -> logger.appendRawLine(line) } },
                ) || changed
            }
            1, 2 -> changed = BeachManager.parseBeachMap(
                html,
                preferences,
                sessionLogger?.let { logger -> { line -> logger.appendRawLine(line) } },
            ) || changed
            3 -> {
                changed = BeachManager.parseBeachHeadCombing(html, preferences) || changed
                changed = BeachManager.parseCombUsage(html, preferences) || changed
            }
            4 -> {
                changed = BeachManager.markCombedSquare(choiceUrl, html, preferences) || changed
                changed = BeachManager.parseCombUsage(html, preferences) || changed
                changed = BeachManager.parseBeachMap(
                    html,
                    preferences,
                    sessionLogger?.let { logger -> { line -> logger.appendRawLine(line) } },
                ) || changed
                if (html.contains("you find a bottle", ignoreCase = true)) {
                    sessionLogger?.appendRawLine("You found a message in a bottle!")
                    changed = true
                }
            }
            5 -> {
                preferences.setBoolean("_beachCombing", false)
                changed = true
            }
            6 -> changed = BeachManager.parseCombUsage(html, preferences) || changed
        }
        return changed
    }
}
