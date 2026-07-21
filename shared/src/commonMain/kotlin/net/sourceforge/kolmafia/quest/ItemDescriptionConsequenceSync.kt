package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ItemDescriptionConsequenceDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ConsequenceManager.parseItemDesc] for desc_item.php HTML. */
object ItemDescriptionConsequenceSync {

    fun applyItemDescription(
        descId: String,
        html: String,
        preferences: Preferences,
    ) {
        for (rule in ItemDescriptionConsequenceDatabase.rulesForDescId(descId)) {
            val match = rule.pattern.find(html) ?: continue
            for (action in rule.actions) {
                ConsequenceActionResolver.fireAction(action, match, preferences)
            }
        }
    }
}
