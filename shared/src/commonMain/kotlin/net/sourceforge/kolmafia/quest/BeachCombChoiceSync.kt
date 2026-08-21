package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BeachCombManager

/** Desktop [ChoiceControl] / [BeachManager] synchronization for choice 1388. */
object BeachCombChoiceSync {
    const val CHOICE_ID = 1388

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = false
        when (decision) {
            0 -> {
                changed = BeachCombManager.parseCombUsage(html, preferences) || changed
                changed = BeachCombManager.parseBeachMap(html, preferences) || changed
            }
            1, 2 -> changed = BeachCombManager.parseBeachMap(html, preferences) || changed
            3 -> {
                changed = BeachCombManager.parseBeachHeadCombing(html, preferences) || changed
                changed = BeachCombManager.parseCombUsage(html, preferences) || changed
            }
            4 -> {
                changed = BeachCombManager.markCombedSquare(choiceUrl, html, preferences) || changed
                changed = BeachCombManager.parseCombUsage(html, preferences) || changed
                changed = BeachCombManager.parseBeachMap(html, preferences) || changed
            }
            5 -> {
                preferences.setBoolean("_beachCombing", false)
                changed = true
            }
            6 -> changed = BeachCombManager.parseCombUsage(html, preferences) || changed
        }
        return changed
    }
}
