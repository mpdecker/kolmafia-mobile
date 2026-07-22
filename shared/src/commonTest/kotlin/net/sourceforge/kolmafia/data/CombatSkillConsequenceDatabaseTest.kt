package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CombatSkillConsequenceDatabaseTest {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
        CombatSkillConsequenceDatabase.resetForTest()
    }

    private fun registerCombatSkill(id: Int, name: String) {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = id,
                name = name,
                image = "test.gif",
                tags = setOf("combat"),
                mpCost = 0,
                duration = 0,
                isPassive = false,
                isCombat = true,
                isNonCombat = false,
                isSong = false,
            ),
        )
    }

    @Test
    fun parseForTest_loadsAllCombatSkillRowsWhenSkillsRegistered() {
        registerCombatSkill(7311, "Use the Force")
        registerCombatSkill(7327, "CHEAT CODE: Shrink Enemy")
        registerCombatSkill(7386, "Fire Extinguisher: Foam 'em Up")
        registerCombatSkill(7309, "Become a Cloud of Mist")
        registerCombatSkill(7291, "Meteor Shower")
        registerCombatSkill(7290, "Macrometeorite")
        registerCombatSkill(7169, "Talk About Politics")
        registerCombatSkill(7305, "Otoscope")
        registerCombatSkill(7306, "Reflex Hammer")
        registerCombatSkill(7307, "Chest X-Ray")
        registerCombatSkill(7381, "Back-Up to your Last Enemy")
        registerCombatSkill(7443, "Cincho: Party Foul")
        registerCombatSkill(226, "Perpetrate Mild Evil")
        registerCombatSkill(7585, "Steal Monster's Heart")

        val parsed = CombatSkillConsequenceDatabase.parseForTest(
            """
            COMBAT_SKILL	Use the Force	Use the Force, [\w ]+! \((\d+) uses? left\)	_saberForceUses=[5-$1]
            COMBAT_SKILL	CHEAT CODE: Shrink Enemy	CHEAT CODE: Shrink Enemy \(5 of today's remaining (\d+)%\)	_powerfulGloveBatteryPowerUsed=[100-$1]
            COMBAT_SKILL	Fire Extinguisher: Foam 'em Up	Fire Extinguisher: Foam 'em Up \(5 charge, (\d+)% remaining\)	_fireExtinguisherCharge=$1
            COMBAT_SKILL	Become a Cloud of Mist	Become a Cloud of Mist \((\d+) time\(s\) remaining today\)\)	_vampyreCloakeFormUses=[10-$1]
            COMBAT_SKILL	Meteor Shower	Meteor Shower \((\d+) charges left\)	_meteorShowerUses=[5-$1]
            COMBAT_SKILL	Macrometeorite	Macrometeorite \((\d+) charges left\)	_macrometeoriteUses=[10-$1]
            COMBAT_SKILL	Talk About Politics	Talk About Politics \((\d+) left\) \(0 Mana Points\)	_pantsgivingBanish=[5-$1]
            COMBAT_SKILL	Otoscope	Otoscope \((\d+) charges left\)	_otoscopeUsed=[3-$1]
            COMBAT_SKILL	Reflex Hammer	Reflex Hammer \((\d+) charges left\)	_reflexHammerUsed=[3-$1]
            COMBAT_SKILL	Chest X-Ray	Chest X-Ray \((\d+) charges left\)	_chestXRayUsed=[3-$1]
            COMBAT_SKILL	Back-Up to your Last Enemy	Back-Up to your Last Enemy \((\d+) uses? today\)	_backUpUses=[(11+path(You, Robot)*5)-$1]
            COMBAT_SKILL	Cincho: Party Foul	Cincho: Party Foul \(5 cinch, (\d+)% remaining\)	_cinchUsed=[100-$1]
            COMBAT_SKILL	Perpetrate Mild Evil	Perpetrate Mild Evil \((\d+) uses? left today	_mildEvilPerpetrated=[3-$1]
            COMBAT_SKILL	Steal Monster's Heart	Steal Monster's Heart \(([A-Z]+) -> [A-Z]+\)	heartstoneLetters=$1
            """.trimIndent(),
        )
        assertEquals(14, parsed.size)
        assertEquals(1, parsed[7311]?.size)
        assertEquals(1, parsed[7585]?.size)
    }

    @Test
    fun parseForTest_skipsUnknownSkill() {
        val parsed = CombatSkillConsequenceDatabase.parseForTest(
            """
            COMBAT_SKILL	Unknown Combat Skill	foo=bar	foo=bar
            """.trimIndent(),
        )
        assertEquals(0, parsed.size)
    }
}
