package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.CombatSkillConsequenceDatabase
import net.sourceforge.kolmafia.data.CombatSkillDropdownParser
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ConsequenceManager.parseCombatSkillName] for fight dropdown labels. */
object CombatSkillConsequenceSync {

    fun applyFromFightHtml(
        html: String,
        preferences: Preferences,
        expressionContext: ExpressionContext = ExpressionContext.EMPTY,
    ) {
        val resolverContext = ConsequenceActionResolver.Context(
            expressionContext = expressionContext,
        )
        for ((skillId, label) in CombatSkillDropdownParser.parseAvailableCombatSkills(html)) {
            for (rule in CombatSkillConsequenceDatabase.rulesForSkillId(skillId)) {
                val match = rule.pattern.find(label) ?: continue
                for (action in rule.actions) {
                    ConsequenceActionResolver.fireAction(
                        action = action,
                        match = match,
                        preferences = preferences,
                        context = resolverContext,
                    )
                }
            }
        }
    }
}
