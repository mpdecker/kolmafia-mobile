package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.QuestLogConsequenceDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ConsequenceManager.parseAccomplishments] for quest log page 3. */
object QuestLogConsequenceSync {

    fun applyAccomplishments(
        html: String,
        preferences: Preferences,
        ascensionNumber: Int = 0,
    ) {
        val context = ConsequenceActionResolver.Context(ascensionNumber = ascensionNumber)
        for (rule in QuestLogConsequenceDatabase.rules()) {
            val match = rule.pattern.find(html) ?: continue
            for (action in rule.actions) {
                ConsequenceActionResolver.fireAction(action, match, preferences, context)
            }
        }
    }
}
