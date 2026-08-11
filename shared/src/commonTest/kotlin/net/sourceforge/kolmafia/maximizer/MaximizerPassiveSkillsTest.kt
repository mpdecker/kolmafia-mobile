package net.sourceforge.kolmafia.maximizer

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType

class MaximizerPassiveSkillsTest {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
    }

    @Test
    fun namesFrom_filtersActiveSkills() {
        val skills = listOf(
            SkillData(1, "Passive One", SkillType.PASSIVE, mpCost = 0, dailyLimit = 0, timesCast = 0),
            SkillData(2, "Active Two", SkillType.NONCOMBAT, mpCost = 10, dailyLimit = 0, timesCast = 0),
        )
        assertEquals(setOf("Passive One"), MaximizerPassiveSkills.namesFrom(skills))
    }

    @Test
    fun isPassiveSkill_vampyreSkill_treatedAsPassive() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 24001,
                name = "Vampyre Passive",
                image = "skill.gif",
                tags = emptySet(),
                mpCost = 0,
                duration = 0,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
        val skill = SkillData(
            id = 24001,
            name = "Vampyre Passive",
            type = SkillType.NONCOMBAT,
            mpCost = 5,
            dailyLimit = 0,
            timesCast = 0,
        )
        assertTrue(MaximizerPassiveSkills.isPassiveSkill(skill))
        assertEquals(setOf("Vampyre Passive"), MaximizerPassiveSkills.resolve(apiSkills = listOf(skill)))
    }

    @Test
    fun resolve_mergesEquipmentGrantedPassive() {
        ModifierDatabase.injectForTest("Skill", "Equip Passive Myst", "Mysticality: +100")
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 9100,
                name = "Equip Passive Myst",
                image = "skill.gif",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
                isNonCombat = false,
                isSong = false,
            ),
        )
        val apiSkills = listOf(
            SkillData(2, "Active Two", SkillType.NONCOMBAT, mpCost = 10, dailyLimit = 0, timesCast = 0),
        )
        val resolved = MaximizerPassiveSkills.resolve(
            apiSkills = apiSkills,
            equipmentGranted = setOf("Equip Passive Myst", "Missing Skill"),
        )
        assertEquals(setOf("Equip Passive Myst"), resolved)
    }
}
