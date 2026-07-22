package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.SkillDescriptionConsequenceDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ConsequenceManager.parseSkillDesc] for desc_skill.php HTML. */
object SkillDescriptionConsequenceSync {

    fun applySkillDescription(
        skillId: Int,
        html: String,
        preferences: Preferences,
    ) {
        for (rule in SkillDescriptionConsequenceDatabase.rulesForSkillId(skillId)) {
            val match = rule.pattern.find(html) ?: continue
            for (action in rule.actions) {
                ConsequenceActionResolver.fireAction(
                    action = action,
                    match = match,
                    preferences = preferences,
                )
            }
        }
    }
}
