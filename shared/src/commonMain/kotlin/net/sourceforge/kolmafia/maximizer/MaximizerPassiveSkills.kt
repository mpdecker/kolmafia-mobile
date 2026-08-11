package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.SkillCategory
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.quest.DynamicItemModifierSync
import net.sourceforge.kolmafia.quest.SkillGrantingEquipmentSync
import net.sourceforge.kolmafia.skill.SkillData

/** Passive skill name derivation for maximizer scoring overlay (Phase 414–415). */
object MaximizerPassiveSkills {

    fun isPassiveSkill(skill: SkillData): Boolean {
        if (!skill.isActive) return true
        val def = SkillDefinitionDatabase.getById(skill.id) ?: return false
        return def.isPassive || SkillDefinitionProxy.getSkillCategory(skill.id) == SkillCategory.VAMPYRE
    }

    fun resolve(
        apiSkills: List<SkillData>,
        equipmentGranted: Set<String> = emptySet(),
    ): Set<String> {
        val api = apiSkills.filter(::isPassiveSkill).map { it.name }
        val equipment = equipmentGranted.filter { name ->
            val def = SkillDefinitionDatabase.getByName(name) ?: return@filter false
            def.isPassive || def.isNonCombat
        }
        return (api + equipment).toSet()
    }

    fun resolve(
        apiSkills: List<SkillData>,
        checkContext: DynamicItemModifierSync.CheckContext,
        gameDatabase: GameDatabase,
    ): Set<String> {
        val equipmentGranted = SkillGrantingEquipmentSync.grantedSkillNames(checkContext, gameDatabase)
        return resolve(apiSkills, equipmentGranted)
    }

    fun namesFrom(skills: List<SkillData>): Set<String> = resolve(apiSkills = skills)
}
