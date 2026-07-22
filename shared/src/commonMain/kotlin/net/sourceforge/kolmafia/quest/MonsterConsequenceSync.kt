package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ConsequenceAction
import net.sourceforge.kolmafia.data.MonsterConsequenceDatabase

/** Desktop [ConsequenceManager.disambiguateMonster] for fight/adventure HTML. */
object MonsterConsequenceSync {

    fun disambiguateMonster(monsterName: String, html: String): String {
        for (rule in MonsterConsequenceDatabase.rulesForMonster(monsterName)) {
            val match = rule.pattern.find(html) ?: continue
            for (action in rule.actions) {
                if (action is ConsequenceAction.ReturnReplacement) {
                    return ConsequenceActionResolver.resolveReplacement(action.template, match)
                }
            }
        }
        return monsterName
    }
}
