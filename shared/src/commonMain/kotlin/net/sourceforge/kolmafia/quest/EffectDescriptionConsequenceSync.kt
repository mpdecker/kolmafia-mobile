package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.EffectDescriptionConsequenceDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ConsequenceManager.parseEffectDesc] for desc_effect.php HTML. */
object EffectDescriptionConsequenceSync {

    fun applyEffectDescription(
        descId: String,
        html: String,
        preferences: Preferences,
    ) {
        for (rule in EffectDescriptionConsequenceDatabase.rulesForDescId(descId)) {
            val match = rule.pattern.find(html) ?: continue
            for (action in rule.actions) {
                ConsequenceActionResolver.fireAction(
                    action = action,
                    match = match,
                    preferences = preferences,
                    effectSpec = rule.spec,
                    html = html,
                )
            }
        }
    }
}
