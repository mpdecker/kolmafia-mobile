package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
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

    @Test fun pickSkillToBurn_noMood_returnsNull() {
        assertNull(
            ManaBurnManager.pickSkillToBurn(null, EffectState(), SkillState(), CharacterState())
        )
    }

    @Test fun pickSkillToBurn_emptyMood_returnsNull() {
        assertNull(
            ManaBurnManager.pickSkillToBurn(
                Mood("x", emptyList()), EffectState(), SkillState(), CharacterState()
            )
        )
    }

    @Test fun pickSkillToBurn_returnsSkillForLowestDurationEffect() {
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

    @Test fun pickSkillToBurn_effectAbsent_treatedAsZeroDuration() {
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

    @Test fun pickSkillToBurn_insufficientMp_skipsSkill() {
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

    @Test fun pickSkillToBurn_zeroMpCostSkill_skipped() {
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

    @Test fun pickSkillToBurn_dailyLimitReached_skipsSkill() {
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

    @Test fun burnIfEnabled_disabled_doesNotCast() {
        val cast = mutableListOf<Int>()
        val manager = ManaBurnManager(fakeCastSkillManager(cast), prefs(enabled = false))
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100")
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 10)))
        runBlocking {
            manager.burnIfEnabled(mood, EffectState(), skillState, CharacterState(currentMp = 100, maxMp = 100))
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun burnIfEnabled_noEligibleSkill_returnsFalse() {
        val cast = mutableListOf<Int>()
        val manager = ManaBurnManager(fakeCastSkillManager(cast), prefs(enabled = true))
        runBlocking {
            val burned = manager.burnIfEnabled(
                mood = null, effectState = EffectState(),
                skillState = SkillState(), charState = CharacterState(currentMp = 100, maxMp = 100)
            )
            assertFalse(burned)
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun burnIfEnabled_castsLowestDurationSkill() {
        val cast = mutableListOf<Int>()
        val manager = ManaBurnManager(fakeCastSkillManager(cast), prefs(enabled = true, belowPct = 90))
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 10)))
        runBlocking {
            val burned = manager.burnIfEnabled(
                mood, EffectState(), skillState, CharacterState(currentMp = 95, maxMp = 100)
            )
            assertTrue(burned)
        }
        assertEquals(listOf(100), cast)
    }

    @Test fun pickSkillToBurn_allowNonMoodBurning_picksBuffSkill() {
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

    @Test fun pickSkillToBurn_summonThreshold_picksSummon() {
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

    @Test fun pickSkillToBurn_priorityListRespected() {
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

    @Test fun skillBurnMinus100_skipsUnextendableEffect() {
        registerCastEffect(9_000_101, "Burn Effect A", "Burn Skill A")
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
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

    @Test fun maxManaBurn_skipsWhenDurationAtLimit() {
        registerCastEffect(9_000_102, "Burn Effect B", "Burn Skill B")
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
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
            )?.id,
        )
    }

    @Test fun onlyMood_skipsNonMoodEffect() {
        registerCastEffect(9_000_103, "Burn Effect C", "Burn Skill C")
        val prefs = Preferences(MapSettings())
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

    @Test fun activeEffectScan_picksLowestDurationEligible() {
        registerCastEffect(9_000_104, "Burn Effect D", "Burn Skill D")
        registerCastEffect(9_000_105, "Burn Effect E", "Burn Skill E")
        val p = MapSettings()
        p.putBoolean(Preferences.ALLOW_NON_MOOD_BURNING, true)
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
            )?.id,
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
