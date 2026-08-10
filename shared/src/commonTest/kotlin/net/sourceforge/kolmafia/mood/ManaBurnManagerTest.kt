package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.inventory.AccessibleItemCount
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import net.sourceforge.kolmafia.skill.SkillType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManaBurnManagerTest {

    @BeforeTest
    fun setUpActiveEffectTests() {
        UneffectSkillEffectMap.resetForTest()
    }

    @AfterTest
    fun tearDownActiveEffectTests() {
        UneffectSkillEffectMap.resetForTest()
        EffectDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
    }

    // ── shouldBurn ────────────────────────────────────────────────────────────

    @Test fun shouldBurn_disabled_returnsFalse() {
        val prefs = prefs(enabled = false)
        assertFalse(ManaBurnManager.shouldBurn(CharacterState(currentMp = 100, maxMp = 100), prefs))
    }

    @Test fun shouldBurn_enabledAboveThreshold_returnsTrue() {
        val prefs = prefs(enabled = true, belowPct = 90)
        assertTrue(ManaBurnManager.shouldBurn(CharacterState(currentMp = 95, maxMp = 100), prefs))
    }

    @Test fun shouldBurn_enabledAtThreshold_returnsTrue() {
        val prefs = prefs(enabled = true, belowPct = 90)
        assertTrue(ManaBurnManager.shouldBurn(CharacterState(currentMp = 90, maxMp = 100), prefs))
    }

    @Test fun shouldBurn_enabledBelowThreshold_returnsFalse() {
        val prefs = prefs(enabled = true, belowPct = 90)
        assertFalse(ManaBurnManager.shouldBurn(CharacterState(currentMp = 89, maxMp = 100), prefs))
    }

    @Test fun shouldBurn_zeroMaxMp_returnsFalse() {
        val prefs = prefs(enabled = true)
        assertFalse(ManaBurnManager.shouldBurn(CharacterState(currentMp = 0, maxMp = 0), prefs))
    }

    // ── pickSkillToBurn ───────────────────────────────────────────────────────

    @Test fun pickSkillToBurn_noMood_returnsNull() = runTest {
        assertNull(
            ManaBurnManager.pickSkillToBurn(null, EffectState(), SkillState(), CharacterState())
        )
    }

    @Test fun pickSkillToBurn_emptyMood_returnsNull() = runTest {
        assertNull(
            ManaBurnManager.pickSkillToBurn(
                Mood("x", emptyList()), EffectState(), SkillState(), CharacterState()
            )
        )
    }

    @Test fun pickSkillToBurn_returnsSkillForLowestDurationEffect() = runTest {
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
            MoodTrigger(effectId = 20, effectName = "E20", skillId = 200, skillName = "S200"),
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 10, name = "E10", duration = 5),
            EffectData(id = 20, name = "E20", duration = 1),   // shorter → burn first
        ))
        val skillState = SkillState(skills = listOf(
            skill(100, mpCost = 10),
            skill(200, mpCost = 10),
        ))
        val picked = ManaBurnManager.pickSkillToBurn(
            mood, effectState, skillState, CharacterState(currentMp = 50)
        )
        assertEquals(200, picked?.id)
    }

    @Test fun pickSkillToBurn_effectAbsent_treatedAsZeroDuration() = runTest {
        // An effect not currently active has 0 duration — should be picked first
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"), // absent
            MoodTrigger(effectId = 20, effectName = "E20", skillId = 200, skillName = "S200"), // 5 turns
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 20, name = "E20", duration = 5),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 10), skill(200, mpCost = 10)))
        val picked = ManaBurnManager.pickSkillToBurn(
            mood, effectState, skillState, CharacterState(currentMp = 50)
        )
        assertEquals(100, picked?.id)
    }

    @Test fun pickSkillToBurn_insufficientMp_skipsSkill() = runTest {
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 100)))
        assertNull(
            ManaBurnManager.pickSkillToBurn(
                mood, EffectState(), skillState, CharacterState(currentMp = 50)
            )
        )
    }

    @Test fun pickSkillToBurn_zeroMpCostSkill_skipped() = runTest {
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 0)))
        assertNull(
            ManaBurnManager.pickSkillToBurn(
                mood, EffectState(), skillState, CharacterState(currentMp = 50)
            )
        )
    }

    @Test fun pickSkillToBurn_dailyLimitReached_skipsSkill() = runTest {
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 10, dailyLimit = 1, timesCast = 1)))
        assertNull(
            ManaBurnManager.pickSkillToBurn(
                mood, EffectState(), skillState, CharacterState(currentMp = 50)
            )
        )
    }

    // ── burnIfEnabled (integration) ───────────────────────────────────────────

    @Test fun burnIfEnabled_disabled_doesNotCast() = runTest {
        val cast = mutableListOf<Int>()
        val manager = ManaBurnManager(fakeCastSkillManager(cast), prefs(enabled = false))
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100")
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 10)))
        manager.burnIfEnabled(mood, EffectState(), skillState, CharacterState(currentMp = 100, maxMp = 100))
        assertTrue(cast.isEmpty())
    }

    @Test fun burnIfEnabled_noEligibleSkill_returnsFalse() = runTest {
        val cast = mutableListOf<Int>()
        val manager = ManaBurnManager(fakeCastSkillManager(cast), prefs(enabled = true))
        val burned = manager.burnIfEnabled(
            mood = null, effectState = EffectState(),
            skillState = SkillState(), charState = CharacterState(currentMp = 100, maxMp = 100)
        )
        assertFalse(burned)
        assertTrue(cast.isEmpty())
    }

    @Test fun burnIfEnabled_castsLowestDurationSkill() = runTest {
        val cast = mutableListOf<Int>()
        val manager = ManaBurnManager(fakeCastSkillManager(cast), prefs(enabled = true, belowPct = 90))
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 10)))
        val burned = manager.burnIfEnabled(
            mood, EffectState(), skillState, CharacterState(currentMp = 95, maxMp = 100)
        )
        assertTrue(burned)
        assertEquals(listOf(100), cast)
    }

    @Test fun pickSkillToBurn_allowNonMoodBurning_picksBuffSkill() = runTest {
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(1, "Arcane Missiles", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val picked = ManaBurnManager.pickSkillToBurn(
            null, EffectState(), skillState, CharacterState(currentMp = 50), prefs = prefs,
        )
        assertEquals(1, picked?.id)
    }

    @Test fun pickSkillToBurn_summonThreshold_picksSummon() = runTest {
        val p = MapSettings()
        p.putInt(Preferences.MANA_BURN_SUMMON_THRESHOLD, 50)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(1, "Summon Snowcone", SkillType.SUMMON, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val picked = ManaBurnManager.pickSkillToBurn(
            null, EffectState(), skillState, CharacterState(currentMp = 80, maxMp = 100), prefs = prefs,
        )
        assertEquals(1, picked?.id)
    }

    @Test fun pickSkillToBurn_priorityListRespected() = runTest {
        val p = MapSettings()
        p.putString(Preferences.MANA_BURN_SKILLS, "Skill B|Skill A")
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(1, "Skill A", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
            SkillData(2, "Skill B", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val picked = ManaBurnManager.pickSkillToBurn(
            null, EffectState(), skillState, CharacterState(currentMp = 50), prefs = prefs,
        )
        assertEquals(2, picked?.id)
    }

    @Test fun skillBurnMinus100_skipsUnextendableEffect() = runTest {
        registerCastEffect(9_000_101, "Burn Effect A", "Burn Skill A")
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, false)
        p.putInt(Preferences.skillBurnPrefKey(9_001), -100)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(9_001, "Burn Skill A", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 9_000_101, name = "Burn Effect A", duration = 1),
        ))
        assertNull(
            ManaBurnManager.pickFromActiveEffects(
                null, effectState, skillState, CharacterState(currentMp = 50), emptyMap(), prefs,
            ),
        )
    }

    @Test fun maxManaBurn_skipsWhenDurationAtLimit() = runTest {
        registerCastEffect(9_000_102, "Burn Effect B", "Burn Skill B")
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, false)
        p.putInt(Preferences.MAX_MANA_BURN, 10)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(9_002, "Burn Skill B", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val atLimit = EffectState(effects = listOf(
            EffectData(id = 9_000_102, name = "Burn Effect B", duration = 10),
        ))
        assertNull(
            ManaBurnManager.pickFromActiveEffects(
                null, atLimit, skillState, CharacterState(currentMp = 50, adventuresLeft = 0), emptyMap(), prefs,
            ),
        )
        val belowLimit = EffectState(effects = listOf(
            EffectData(id = 9_000_102, name = "Burn Effect B", duration = 9),
        ))
        assertEquals(
            9_002,
            ManaBurnManager.pickFromActiveEffects(
                null, belowLimit, skillState, CharacterState(currentMp = 50, adventuresLeft = 0), emptyMap(), prefs,
            ).castPick()?.skill?.id,
        )
    }

    @Test fun onlyMood_skipsNonMoodEffect() = runTest {
        registerCastEffect(9_000_103, "Burn Effect C", "Burn Skill C")
        val prefs = Preferences(MapSettings().apply {
            putBoolean(Preferences.ALLOW_SUMMON_BURNING, false)
        })
        val skillState = SkillState(skills = listOf(
            SkillData(9_003, "Burn Skill C", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 9_000_103, name = "Burn Effect C", duration = 1),
        ))
        val mood = Mood("run", listOf(
            MoodTrigger(effectId = 99, effectName = "Other", skillId = 99, skillName = "Other Skill"),
        ))
        assertNull(
            ManaBurnManager.pickFromActiveEffects(
                mood, effectState, skillState, CharacterState(currentMp = 50), emptyMap(), prefs,
            ),
        )
    }

    @Test fun activeEffectScan_picksLowestDurationEligible() = runTest {
        registerCastEffect(9_000_104, "Burn Effect D", "Burn Skill D")
        registerCastEffect(9_000_105, "Burn Effect E", "Burn Skill E")
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, false)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(9_004, "Burn Skill D", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
            SkillData(9_005, "Burn Skill E", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 9_000_104, name = "Burn Effect D", duration = 5),
            EffectData(id = 9_000_105, name = "Burn Effect E", duration = 1),
        ))
        assertEquals(
            9_005,
            ManaBurnManager.pickFromActiveEffects(
                null, effectState, skillState, CharacterState(currentMp = 50), emptyMap(), prefs,
            ).castPick()?.skill?.id,
        )
    }

    @Test fun considerBreakfastSkill_picksFirstEligibleBreakfastSkill() {
        val p = MapSettings()
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 50)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(3006, "Pastamastery", SkillType.SUMMON, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val pick = ManaBurnManager.considerBreakfastSkill(
            skillState,
            CharacterState(currentMp = 100, maxMp = 100),
            prefs,
        ).castPick()
        assertEquals(3006, pick?.skill?.id)
        assertEquals(10, pick?.quantity)
    }

    @Test fun considerBreakfastSkill_skipsPastamasteryWhenCannotEat() {
        val p = MapSettings()
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 50)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(3006, "Pastamastery", SkillType.SUMMON, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        assertNull(
            ManaBurnManager.considerBreakfastSkill(
                skillState,
                CharacterState(
                    currentMp = 100,
                    maxMp = 100,
                    challengePath = net.sourceforge.kolmafia.character.AscensionPath.OXYGENARIAN.apiName,
                ),
                prefs,
            ),
        )
    }

    @Test fun pickFromActiveEffects_summonThreshold_returnsBreakfastEarly() = runTest {
        registerCastEffect(9_000_106, "Burn Effect F", "Burn Skill F")
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, true)
        p.putInt(Preferences.MANA_BURN_SUMMON_THRESHOLD, 10)
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 50)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(9_006, "Burn Skill F", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
            SkillData(3006, "Pastamastery", SkillType.SUMMON, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 9_000_106, name = "Burn Effect F", duration = 10),
        ))
        val action = ManaBurnManager.pickFromActiveEffects(
            null, effectState, skillState, CharacterState(currentMp = 100, maxMp = 100), emptyMap(), prefs,
        )
        assertEquals(3006, action.castPick()?.skill?.id)
    }

    @Test fun pickFromActiveEffects_clearsBreakfastWhenEffectExtendable() = runTest {
        registerCastEffect(9_000_107, "Burn Effect G", "Burn Skill G")
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, true)
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 50)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(9_007, "Burn Skill G", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
            SkillData(3006, "Pastamastery", SkillType.SUMMON, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 9_000_107, name = "Burn Effect G", duration = 1),
        ))
        val action = ManaBurnManager.pickFromActiveEffects(
            null, effectState, skillState, CharacterState(currentMp = 100, maxMp = 100), emptyMap(), prefs,
        )
        assertEquals(9_007, action.castPick()?.skill?.id)
    }

    @Test fun pickFromActiveEffects_closetOnlyBuffTool_usesPhysicalAccessibleCount() = runTest {
        val toolId = 2558
        val skillId = 2101
        registerCastEffect(9_000_109, "Burn Effect I", "Burn Skill I")
        registerTtBuffSkill(skillId, duration = 5)
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, false)
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 50)
        p.putInt(Preferences.MAX_MANA_BURN, 20)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(skillId, "Burn Skill I", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 9_000_109, name = "Burn Effect I", duration = 1),
        ))
        val closet = FakeClosetRequest(mapOf(toolId to 1))
        val accessibleCount: suspend (Int) -> Int = { itemId ->
            AccessibleItemCount.physicalCount(
                itemId = itemId,
                itemName = "chelonian morningstar",
                inventoryManager = emptyInventoryManager(),
                closetRequest = closet,
                storageRequest = null,
                displayCaseRequest = null,
                clanStashRequest = null,
                equipment = emptyMap(),
            )
        }
        val withoutCloset = ManaBurnManager.pickFromActiveEffects(
            null, effectState, skillState, CharacterState(currentMp = 100, maxMp = 100), emptyMap(), prefs,
        ).castPick()
        val withCloset = ManaBurnManager.pickFromActiveEffects(
            null, effectState, skillState, CharacterState(currentMp = 100, maxMp = 100), emptyMap(), prefs,
            accessibleCount = accessibleCount,
        ).castPick()
        assertEquals(skillId, withoutCloset?.skill?.id)
        assertEquals(skillId, withCloset?.skill?.id)
        assertTrue(withCloset!!.quantity < withoutCloset!!.quantity)
    }

    @Test fun pickFromActiveEffects_skipsPastamancerThrallBind() = runTest {
        registerCastEffect(
            1443,
            "Flimsy Shield of the Pastalord",
            "Bind Shield Skill",
        )
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 50)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(9_001, "Bind Shield Skill", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 1443, name = "Flimsy Shield of the Pastalord", duration = 1),
        ))
        assertNull(
            ManaBurnManager.pickFromActiveEffects(
                null,
                effectState,
                skillState,
                CharacterState(
                    currentMp = 100,
                    maxMp = 100,
                    characterClass = net.sourceforge.kolmafia.character.CharacterClass.PASTAMANCER.id,
                ),
                emptyMap(),
                prefs,
            ),
        )
    }

    @Test fun buildLibramSummonCommand_rotatesAcrossSkills() {
        val command = LibramSkillCasts.buildLibramSummonCommand(
            totalCasts = 3,
            castable = listOf("Summon Candy Heart", "Summon Party Favor"),
            nextCastIndex = 1,
        )
        assertEquals("cast 2 Summon Party Favor;cast 1 Summon Candy Heart;", command)
    }

    @Test fun considerLibramSummon_multiSkill_returnsCliCommand() {
        val p = MapSettings()
        p.putString(Preferences.LIBRAM_SKILLS_SOFTCORE, "all")
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 10)
        p.putInt(Preferences.LIBRAM_SUMMONS, 1)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(7219, "Summon Candy Heart", SkillType.SUMMON, mpCost = 1, dailyLimit = 0, timesCast = 0),
            SkillData(7220, "Summon Party Favor", SkillType.SUMMON, mpCost = 1, dailyLimit = 0, timesCast = 0),
        ))
        val action = ManaBurnManager.considerLibramSummon(
            skillState,
            CharacterState(currentMp = 100, maxMp = 100),
            prefs,
        )
        assertTrue(action is ManaBurnAction.Cli)
        assertTrue((action as ManaBurnAction.Cli).command.contains("Summon Candy Heart"))
        assertTrue(action.command.contains("Summon Party Favor"))
    }

    @Test fun considerLibramSummon_singleSkill_returnsCast() {
        val p = MapSettings()
        p.putString(Preferences.LIBRAM_SKILLS_SOFTCORE, "Summon Candy Heart")
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 10)
        p.putInt(Preferences.LIBRAM_SUMMONS, 0)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(7219, "Summon Candy Heart", SkillType.SUMMON, mpCost = 1, dailyLimit = 0, timesCast = 0),
        ))
        val action = ManaBurnManager.considerLibramSummon(
            skillState,
            CharacterState(currentMp = 100, maxMp = 100),
            prefs,
        )
        assertTrue(action is ManaBurnAction.Cast)
        assertEquals(7219, (action as ManaBurnAction.Cast).pick.skill.id)
        assertTrue(action.pick.quantity > 0)
    }

    @Test fun considerLibramSummon_manaCostAdjustment_reducesCastCount() {
        val p = MapSettings()
        p.putString(Preferences.LIBRAM_SKILLS_SOFTCORE, "Summon Candy Heart")
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 0)
        p.putInt(Preferences.LIBRAM_SUMMONS, 0)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(7219, "Summon Candy Heart", SkillType.SUMMON, mpCost = 1, dailyLimit = 0, timesCast = 0),
        ))
        val charState = CharacterState(currentMp = 10, maxMp = 10)
        val withoutAdjustment = ManaBurnManager.considerLibramSummon(
            skillState, charState, prefs, manaCostAdjustment = 0,
        ) as ManaBurnAction.Cast
        val withAdjustment = ManaBurnManager.considerLibramSummon(
            skillState, charState, prefs, manaCostAdjustment = 3,
        ) as ManaBurnAction.Cast
        assertEquals(3, withoutAdjustment.pick.quantity)
        assertEquals(2, withAdjustment.pick.quantity)
    }

    @Test fun resolveBurnAction_multiLibram_returnsCliWhenNoExtendableEffects() = runTest {
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, true)
        p.putString(Preferences.LIBRAM_SKILLS_SOFTCORE, "all")
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 10)
        p.putInt(Preferences.LIBRAM_SUMMONS, 1)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(7219, "Summon Candy Heart", SkillType.SUMMON, mpCost = 1, dailyLimit = 0, timesCast = 0),
            SkillData(7220, "Summon Party Favor", SkillType.SUMMON, mpCost = 1, dailyLimit = 0, timesCast = 0),
        ))
        val action = ManaBurnManager.resolveBurnAction(
            mood = null,
            effectState = EffectState(),
            skillState = skillState,
            charState = CharacterState(currentMp = 100, maxMp = 100),
            moodLibrary = emptyMap(),
            prefs = prefs,
        )
        assertTrue(action is ManaBurnAction.Cli)
    }

    @Test fun burnIfEnabled_invokesCliExecutorForMultiLibram() = runTest {
        val p = MapSettings()
        p.putBoolean(Preferences.MANA_BURN_ENABLED, true)
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 10)
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, true)
        p.putString(Preferences.LIBRAM_SKILLS_SOFTCORE, "all")
        p.putInt(Preferences.LIBRAM_SUMMONS, 1)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(7219, "Summon Candy Heart", SkillType.SUMMON, mpCost = 1, dailyLimit = 0, timesCast = 0),
            SkillData(7220, "Summon Party Favor", SkillType.SUMMON, mpCost = 1, dailyLimit = 0, timesCast = 0),
        ))
        val commands = mutableListOf<String>()
        val manager = ManaBurnManager(fakeCastSkillManager(mutableListOf()), prefs)
        manager.cliExecutor = { commands.add(it) }
        val burned = manager.burnIfEnabled(
            mood = null,
            effectState = EffectState(),
            skillState = skillState,
            charState = CharacterState(currentMp = 100, maxMp = 100),
        )
        assertTrue(burned)
        assertEquals(1, commands.size)
        assertTrue(commands[0].contains(';'))
        assertTrue(commands[0].contains("Summon Candy Heart"))
        assertTrue(commands[0].contains("Summon Party Favor"))
    }

    // ── Phase 352 last-chance burn ────────────────────────────────────────────

    @Test fun considerLastChanceBurn_replacesHashWithAllowedMp() {
        val p = MapSettings()
        p.putString(Preferences.LAST_CHANCE_BURN, "cast # Extra Skill")
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 50)
        val prefs = Preferences(p)
        val charState = CharacterState(currentMp = 200, maxMp = 200)
        assertEquals(
            "cast 100 Extra Skill",
            ManaBurnManager.considerLastChanceBurn(charState, prefs),
        )
    }

    @Test fun considerLastChanceBurn_rejectsEmptyAndBurnPrefix() {
        val p = MapSettings()
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 10)
        p.putInt(Preferences.LAST_CHANCE_THRESHOLD, 10)
        val charState = CharacterState(currentMp = 100, maxMp = 100)
        p.putString(Preferences.LAST_CHANCE_BURN, "")
        assertNull(ManaBurnManager.considerLastChanceBurn(charState, Preferences(p)))
        p.putString(Preferences.LAST_CHANCE_BURN, "burn 5 skill")
        assertNull(ManaBurnManager.considerLastChanceBurn(charState, Preferences(p)))
    }

    @Test fun considerLastChanceBurn_skipsWhenAllowedMpBelowThreshold() {
        val p = MapSettings()
        p.putString(Preferences.LAST_CHANCE_BURN, "cast # skill")
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 50)
        p.putInt(Preferences.LAST_CHANCE_THRESHOLD, 100)
        val prefs = Preferences(p)
        val charState = CharacterState(currentMp = 100, maxMp = 100)
        assertNull(ManaBurnManager.considerLastChanceBurn(charState, prefs))
    }

    @Test fun pickBurnPick_fallsThroughToLastChanceAfterOtherFallbacksFail() = runTest {
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, false)
        p.putString(Preferences.LAST_CHANCE_BURN, "visit campground")
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 10)
        p.putInt(Preferences.LAST_CHANCE_THRESHOLD, 10)
        val prefs = Preferences(p)
        val action = ManaBurnManager.resolveBurnAction(
            mood = null,
            effectState = EffectState(),
            skillState = SkillState(),
            charState = CharacterState(currentMp = 100, maxMp = 100),
            moodLibrary = emptyMap(),
            prefs = prefs,
        )
        assertEquals(ManaBurnAction.Cli("visit campground"), action)
    }

    @Test fun burnIfEnabled_invokesCliExecutorForLastChanceCommand() = runTest {
        val p = MapSettings()
        p.putBoolean(Preferences.MANA_BURN_ENABLED, true)
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 10)
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, false)
        p.putString(Preferences.LAST_CHANCE_BURN, "echo burned")
        p.putInt(Preferences.LAST_CHANCE_THRESHOLD, 10)
        val prefs = Preferences(p)
        val commands = mutableListOf<String>()
        val manager = ManaBurnManager(fakeCastSkillManager(mutableListOf()), prefs)
        manager.cliExecutor = { commands.add(it) }
        val burned = manager.burnIfEnabled(
            mood = null,
            effectState = EffectState(),
            skillState = SkillState(),
            charState = CharacterState(currentMp = 100, maxMp = 100),
        )
        assertTrue(burned)
        assertEquals(listOf("echo burned"), commands)
    }

    @Test fun pickFromActiveEffects_returnsSimulatedQuantityGreaterThanOne() = runTest {
        registerCastEffect(9_000_108, "Burn Effect H", "Burn Skill H")
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
        p.putBoolean(Preferences.ALLOW_SUMMON_BURNING, false)
        p.putInt(Preferences.MANA_BURN_MIN_MP_PCT, 50)
        val prefs = Preferences(p)
        val skillState = SkillState(skills = listOf(
            SkillData(9_008, "Burn Skill H", SkillType.BUFF, mpCost = 5, dailyLimit = 0, timesCast = 0),
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 9_000_108, name = "Burn Effect H", duration = 1),
        ))
        val pick = ManaBurnManager.pickFromActiveEffects(
            null, effectState, skillState, CharacterState(currentMp = 100, maxMp = 100), emptyMap(), prefs,
        ).castPick()
        assertEquals(9_008, pick?.skill?.id)
        assertEquals(10, pick?.quantity)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun ManaBurnAction?.castPick(): ManaBurnPick? = (this as? ManaBurnAction.Cast)?.pick

    private fun registerTtBuffSkill(skillId: Int, duration: Int) {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = skillId,
                name = "Test TT Buff $skillId",
                image = "buff.gif",
                tags = setOf("other", "nc"),
                mpCost = 5,
                duration = duration,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
    }

    private class FakeClosetRequest(
        private val contents: Map<Int, Int>,
    ) : ClosetRequest(io.ktor.client.HttpClient(MockEngine { respond("") })) {
        override suspend fun fetchContents(): Map<Int, Int> = contents
    }

    private class EmptyInventoryManager :
        InventoryManager(io.ktor.client.HttpClient(MockEngine { respond("") }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = emptyMap()))
        override val state = flow.asStateFlow()
    }

    private fun emptyInventoryManager() = EmptyInventoryManager()

    private fun prefs(enabled: Boolean, belowPct: Int = 90): Preferences {
        val s = MapSettings()
        s.putBoolean(Preferences.MANA_BURN_ENABLED, enabled)
        s.putInt(Preferences.MANA_BURN_MIN_MP_PCT, belowPct)
        return Preferences(s)
    }

    private fun registerCastEffect(effectId: Int, effectName: String, skillName: String) {
        EffectDatabase.registerForTest(
            net.sourceforge.kolmafia.data.EffectData(
                id = effectId,
                name = effectName,
                image = "test.gif",
                descId = "test",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "cast 1 $skillName",
            ),
        )
        UneffectSkillEffectMap.rebuild()
    }

    private fun skill(
        id: Int,
        mpCost: Int,
        dailyLimit: Int = 0,
        timesCast: Int = 0,
    ) = SkillData(
        id = id, name = "Skill $id",
        type = SkillType.PASSIVE,
        mpCost = mpCost, dailyLimit = dailyLimit, timesCast = timesCast,
    )

    private fun fakeCastSkillManager(cast: MutableList<Int>): SkillManager {
        val fakeClient = io.ktor.client.HttpClient(MockEngine { _ -> respond("") })
        val fakeRequest = SkillCastRequest(fakeClient)
        val fakeEventBus = GameEventBus()
        return object : SkillManager(fakeClient, fakeRequest, fakeEventBus) {
            override suspend fun cast(skill: SkillData, quantity: Int): Result<Unit> {
                cast.add(skill.id)
                return Result.success(Unit)
            }
        }
    }
}
