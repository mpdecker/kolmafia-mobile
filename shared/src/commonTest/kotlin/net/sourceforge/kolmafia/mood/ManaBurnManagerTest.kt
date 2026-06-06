package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import net.sourceforge.kolmafia.skill.SkillType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManaBurnManagerTest {

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun prefs(enabled: Boolean, belowPct: Int = 90): Preferences {
        val s = MapSettings()
        s.putBoolean(Preferences.MANA_BURN_ENABLED, enabled)
        s.putInt(Preferences.MANA_BURN_MIN_MP_PCT, belowPct)
        return Preferences(s)
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
