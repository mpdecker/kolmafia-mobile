package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillLearner
import net.sourceforge.kolmafia.skill.SkillManager

/** Desktop [ResponseTextParser.learnSkillFromResponse]. */
object SkillLearnFromResponse {

    private val NEW_SKILL_BY_NAME =
        Regex("""<td>You (?:have learned|learn) a new skill: <b>(.*?)</b>""")
    private val NEW_SKILL_BY_ID =
        Regex("""You (?:gain|acquire) a skill:.*?whichskill=(\d+)""")

    fun learnSkillFromResponse(
        html: String,
        preferences: Preferences,
        skillManager: SkillManager?,
        inventoryManager: InventoryManager?,
    ): Int {
        var skillFound = 0
        for (match in NEW_SKILL_BY_NAME.findAll(html)) {
            val name = match.groupValues[1]
            val skillId = SkillDefinitionDatabase.getByName(name)?.id ?: continue
            skillFound = SkillLearner.learnSkill(
                skillId,
                preferences,
                skillManager,
                inventoryManager = inventoryManager,
            )
        }
        if (skillFound != 0) {
            return skillFound
        }
        for (match in NEW_SKILL_BY_ID.findAll(html)) {
            val skillId = match.groupValues[1].toIntOrNull() ?: continue
            skillFound = SkillLearner.learnSkill(
                skillId,
                preferences,
                skillManager,
                inventoryManager = inventoryManager,
            )
        }
        return skillFound
    }
}
