package net.sourceforge.kolmafia.mood

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillState
import net.sourceforge.kolmafia.skill.SkillType

class ManaBurnSimulationTest {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test fun simulateCast_incrementsDurationAndCount() {
        val burn = ManaBurn(
            skillId = 1,
            skillName = "Test Skill",
            effectName = "Test Effect",
            duration = 5,
            limit = 100,
            mpCost = 10,
            effectDurationPerCast = 3,
        )
        assertEquals(10L, burn.simulateCast())
        assertEquals(1, burn.count)
        assertEquals(8, burn.duration)
    }

    @Test fun simulateBalancedCasts_distributesAcrossTwoEffects() {
        val chosen = ManaBurn(
            skillId = 1,
            skillName = "Cheap",
            effectName = "Effect A",
            duration = 1,
            limit = 10_000,
            mpCost = 5,
            effectDurationPerCast = 1,
        )
        val other = ManaBurn(
            skillId = 2,
            skillName = "Other",
            effectName = "Effect B",
            duration = 5,
            limit = 10_000,
            mpCost = 5,
            effectDurationPerCast = 1,
        )
        val burns = mutableListOf(chosen, other)
        ManaBurn.simulateBalancedCasts(burns, allowedMp = 100)
        assertTrue(chosen.count > 1)
        assertTrue(other.count > 0)
        assertEquals(20, chosen.count + other.count)
    }

    @Test fun getEffectDuration_doublesSongsWithGoodSingingVoice() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 6001,
                name = "Test Song",
                image = "song.gif",
                tags = setOf("song", "other"),
                mpCost = 10,
                duration = 15,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = true,
            ),
        )
        val emptyChar = CharacterState()
        val emptyEffects = EffectState()
        val withoutVoice = SkillState(skills = emptyList())
        assertEquals(
            15,
            SkillDefinitionProxy.getEffectDuration(6001, withoutVoice, emptyChar, emptyEffects),
        )

        val withVoice = SkillState(
            skills = listOf(
                SkillData(11_016, "Good Singing Voice", SkillType.PASSIVE, mpCost = 0, dailyLimit = 0, timesCast = 0),
            ),
        )
        assertEquals(
            30,
            SkillDefinitionProxy.getEffectDuration(6001, withVoice, emptyChar, emptyEffects),
        )
    }

    @Test fun getEffectDuration_spiritBoon_usesBlessingLevel() {
        registerNonBuffSkill(SPIRIT_BOON, duration = 5)
        val effectState = EffectState(
            effects = listOf(EffectData(id = 1417, name = "Grand Blessing of the War Snapper", duration = 10)),
        )
        assertEquals(
            10,
            SkillDefinitionProxy.getEffectDuration(
                SPIRIT_BOON,
                SkillState(skills = emptyList()),
                CharacterState(),
                effectState,
            ),
        )
    }

    @Test fun getEffectDuration_ttBlessing_nonTamerReturnsTen() {
        registerNonBuffSkill(WAR_BLESSING, duration = 15)
        assertEquals(
            10,
            SkillDefinitionProxy.getEffectDuration(
                WAR_BLESSING,
                SkillState(skills = emptyList()),
                CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id),
                EffectState(),
            ),
        )
    }

    @Test fun getEffectDuration_pastaBind_pastamancerReturnsZero() {
        registerNonBuffSkill(BIND_VAMPIEROGHI, duration = 12)
        assertEquals(
            0,
            SkillDefinitionProxy.getEffectDuration(
                BIND_VAMPIEROGHI,
                SkillState(skills = emptyList()),
                CharacterState(characterClass = CharacterClass.PASTAMANCER.id),
                EffectState(),
            ),
        )
    }

    @Test fun getEffectDuration_pastaBind_nonPastamancerReturnsTen() {
        registerNonBuffSkill(BIND_VAMPIEROGHI, duration = 12)
        assertEquals(
            10,
            SkillDefinitionProxy.getEffectDuration(
                BIND_VAMPIEROGHI,
                SkillState(skills = emptyList()),
                CharacterState(characterClass = CharacterClass.TURTLE_TAMER.id),
                EffectState(),
            ),
        )
    }

    @Test fun getEffectDuration_revEngine_usesAudience() {
        registerNonBuffSkill(REV_ENGINE, duration = 5)
        assertEquals(
            12,
            SkillDefinitionProxy.getEffectDuration(
                REV_ENGINE,
                SkillState(skills = emptyList()),
                CharacterState(audience = -12),
                EffectState(),
            ),
        )
    }

    @Test fun getEffectDuration_bikerSwagger_usesAudienceFloorTen() {
        registerNonBuffSkill(BIKER_SWAGGER, duration = 30)
        assertEquals(
            10,
            SkillDefinitionProxy.getEffectDuration(
                BIKER_SWAGGER,
                SkillState(skills = emptyList()),
                CharacterState(audience = 3),
                EffectState(),
            ),
        )
    }

    @Test fun getEffectDuration_wizardHatEquipped_addsFive() {
        registerBuffSkill(2100, duration = 10)
        val charState = CharacterState(
            equipment = mapOf(EquipmentSlot.HAT to "jewel-eyed wizard hat"),
        )
        assertEquals(
            15,
            SkillDefinitionProxy.getEffectDuration(
                2100,
                SkillState(skills = emptyList()),
                charState,
                EffectState(),
            ),
        )
    }

    @Test fun getEffectDuration_turtleTamerBuff_usesBestOwnedTool() {
        registerBuffSkill(2101, duration = 5)
        val charState = CharacterState()
        val counts = mutableMapOf(7 to 1)
        assertEquals(
            5,
            SkillDefinitionProxy.getEffectDuration(
                2101,
                SkillState(skills = emptyList()),
                charState,
                EffectState(),
                accessibleCount = { counts[it] ?: 0 },
            ),
        )
        counts[2558] = 1
        assertEquals(
            15,
            SkillDefinitionProxy.getEffectDuration(
                2101,
                SkillState(skills = emptyList()),
                charState,
                EffectState(),
                accessibleCount = { counts[it] ?: 0 },
            ),
        )
    }

    @Test fun getEffectDuration_accordionThiefSong_usesEquippedTool() {
        registerBuffSkill(6002, duration = 5, isSong = true)
        registerAccordionItem(4321, "Trickster Trikitixa")
        val charState = CharacterState(
            characterClass = CharacterClass.ACCORDION_THIEF.id,
            equipment = mapOf(EquipmentSlot.WEAPON to "Trickster Trikitixa"),
        )
        val gameDatabase = GameDatabase()
        assertEquals(
            20,
            SkillDefinitionProxy.getEffectDuration(
                6002,
                SkillState(skills = emptyList()),
                charState,
                EffectState(),
                gameDatabase = gameDatabase,
            ),
        )
    }

    @Test fun getEffectDuration_classLimitedTool_ignoredForWrongClass() {
        registerBuffSkill(6003, duration = 5, isSong = true)
        registerAccordionItem(4321, "Trickster Trikitixa")
        val charState = CharacterState(
            characterClass = CharacterClass.TURTLE_TAMER.id,
            equipment = mapOf(EquipmentSlot.WEAPON to "Trickster Trikitixa"),
        )
        val gameDatabase = GameDatabase()
        assertEquals(
            5,
            SkillDefinitionProxy.getEffectDuration(
                6003,
                SkillState(skills = emptyList()),
                charState,
                EffectState(),
                gameDatabase = gameDatabase,
            ),
        )
    }

    private fun registerBuffSkill(skillId: Int, duration: Int, isSong: Boolean = false) {
        val tags = buildSet {
            add("other")
            add("nc")
            if (isSong) add("song")
        }
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = skillId,
                name = "Test Buff $skillId",
                image = "buff.gif",
                tags = tags,
                mpCost = 10,
                duration = duration,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = isSong,
            ),
        )
    }

    private fun registerAccordionItem(itemId: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = itemId,
                name = name,
                descId = "d$itemId",
                image = "accordion.gif",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 0,
                plural = null,
            ),
        )
    }

    private fun registerNonBuffSkill(skillId: Int, duration: Int) {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = skillId,
                name = "Test Skill $skillId",
                image = "test.gif",
                tags = setOf("nc", "effect", "self"),
                mpCost = 10,
                duration = duration,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
    }

    private companion object {
        const val SPIRIT_BOON = 2039
        const val WAR_BLESSING = 2030
        const val BIND_VAMPIEROGHI = 3027
        const val REV_ENGINE = 15011
        const val BIKER_SWAGGER = 15019
    }
}
