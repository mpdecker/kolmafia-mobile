package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.CombatSkillDropdownParser
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase

/**
 * Desktop [KoLConstants.availableCombatSkillsSet] — fight-dropdown combat skills.
 * Updated from fight HTML via [CombatSkillDropdownParser].
 */
object AvailableCombatSkills {
    private val skillIds = linkedSetOf<Int>()

    fun clear() {
        skillIds.clear()
    }

    fun setFromFightHtml(html: String) {
        skillIds.clear()
        for ((id, _) in CombatSkillDropdownParser.parseAvailableCombatSkills(html)) {
            skillIds += id
        }
    }

    fun has(skillId: Int): Boolean = skillId > 0 && skillId in skillIds

    fun hasName(skillName: String): Boolean {
        if (skillName.isBlank()) return false
        val byName = SkillDefinitionDatabase.getByName(skillName)?.id
        if (byName != null && byName in skillIds) return true
        return skillIds.any { id ->
            SkillDefinitionDatabase.getById(id)?.name.equals(skillName, ignoreCase = true)
        }
    }

    fun ids(): Set<Int> = skillIds.toSet()
}
