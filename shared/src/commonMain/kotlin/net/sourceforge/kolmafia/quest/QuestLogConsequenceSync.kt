package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.QuestLogConsequenceAction
import net.sourceforge.kolmafia.data.QuestLogConsequenceDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ConsequenceManager.parseAccomplishments] for quest log page 3. */
object QuestLogConsequenceSync {

    fun applyAccomplishments(
        html: String,
        preferences: Preferences,
        ascensionNumber: Int = 0,
    ) {
        for (rule in QuestLogConsequenceDatabase.rules()) {
            val match = rule.pattern.find(html) ?: continue
            for (action in rule.actions) {
                fireAction(action, match, preferences, ascensionNumber)
            }
        }
    }

    private fun fireAction(
        action: QuestLogConsequenceAction,
        match: MatchResult,
        preferences: Preferences,
        ascensionNumber: Int,
    ) {
        when (action) {
            is QuestLogConsequenceAction.SetString -> {
                val value = match.groupValues.getOrNull(action.groupIndex) ?: return
                preferences.setString(action.key, value)
            }
            is QuestLogConsequenceAction.SetLiteral ->
                preferences.setString(action.key, action.value)
            is QuestLogConsequenceAction.SetBoolean ->
                preferences.setBoolean(action.key, action.value)
            is QuestLogConsequenceAction.SetAscensions ->
                preferences.setInt(action.key, ascensionNumber)
        }
    }
}
