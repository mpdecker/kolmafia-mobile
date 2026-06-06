package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoodManagerTest {

    private fun prefs(autoBuffEnabled: Boolean = true): Preferences {
        val s = MapSettings()
        s.putBoolean(Preferences.AUTO_BUFF, autoBuffEnabled)
        return Preferences(s)
    }

    private fun trigger(effectId: Int, skillId: Int, minTurns: Int = 1) =
        MoodTrigger(effectId, "Effect $effectId", skillId, "Skill $skillId", minTurns)

    private fun effect(id: Int, duration: Int) =
        EffectData(id = id, name = "Effect $id", duration = duration)

    private fun effectState(vararg effects: EffectData) =
        EffectState(effects = effects.toList())

    // ── missingTriggers ──────────────────────────────────────────────────────

    @Test fun missingTriggers_effectPresent_returnsEmpty() {
        val mood = Mood("test", listOf(trigger(effectId = 10, skillId = 200, minTurns = 1)))
        val effects = effectState(effect(10, duration = 3))
        val missing = MoodManager.missingTriggers(mood, effects)
        assertTrue(missing.isEmpty())
    }

    @Test fun missingTriggers_effectAbsent_returnsTrigger() {
        val t = trigger(effectId = 10, skillId = 200)
        val mood = Mood("test", listOf(t))
        val missing = MoodManager.missingTriggers(mood, effectState())
        assertEquals(listOf(t), missing)
    }

    @Test fun missingTriggers_effectBelowMinTurns_returnsTrigger() {
        val t = trigger(effectId = 10, skillId = 200, minTurns = 5)
        val mood = Mood("test", listOf(t))
        val effects = effectState(effect(10, duration = 3))
        val missing = MoodManager.missingTriggers(mood, effects)
        assertEquals(listOf(t), missing)
    }

    @Test fun missingTriggers_exactlyAtMinTurns_returnsEmpty() {
        val t = trigger(effectId = 10, skillId = 200, minTurns = 3)
        val mood = Mood("test", listOf(t))
        val effects = effectState(effect(10, duration = 3))
        assertTrue(MoodManager.missingTriggers(mood, effects).isEmpty())
    }

    @Test fun missingTriggers_multipleTriggers_returnsMissingOnly() {
        val t1 = trigger(effectId = 10, skillId = 200)
        val t2 = trigger(effectId = 20, skillId = 300)
        val mood = Mood("test", listOf(t1, t2))
        val effects = effectState(effect(10, duration = 5))  // only t1 present
        assertEquals(listOf(t2), MoodManager.missingTriggers(mood, effects))
    }

    // ── executeActiveMood (captures cast calls) ──────────────────────────────

    @Test fun executeActiveMood_noActiveMood_doesNothing() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = null
        runBlocking {
            manager.executeActiveMood(effectState(), SkillState(), CharacterState())
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun executeActiveMood_autoBuff_disabled_doesNothing() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs(autoBuffEnabled = false))
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        runBlocking {
            manager.executeActiveMood(effectState(), SkillState(), CharacterState())
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun executeActiveMood_missingEffect_castsSkill() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        val skills = SkillState(skills = listOf(skillData(id = 200, mpCost = 10)))
        runBlocking {
            manager.executeActiveMood(effectState(), skills, CharacterState(currentMp = 50, maxMp = 100))
        }
        assertEquals(listOf(200), cast)
    }

    @Test fun executeActiveMood_effectPresent_doesNotCast() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        val skills = SkillState(skills = listOf(skillData(id = 200, mpCost = 10)))
        runBlocking {
            manager.executeActiveMood(effectState(effect(10, 3)), skills, CharacterState(currentMp = 50, maxMp = 100))
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun executeActiveMood_insufficientMp_skipsSkill() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        val skills = SkillState(skills = listOf(skillData(id = 200, mpCost = 50)))
        runBlocking {
            manager.executeActiveMood(effectState(), skills, CharacterState(currentMp = 10, maxMp = 100))
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun executeActiveMood_skillNotKnown_skipsIt() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        runBlocking {
            manager.executeActiveMood(effectState(), SkillState(skills = emptyList()), CharacterState(currentMp = 50, maxMp = 100))
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun executeActiveMood_dailyLimitReached_skipsSkill() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        val skills = SkillState(skills = listOf(skillData(id = 200, mpCost = 10, dailyLimit = 1, timesCast = 1)))
        runBlocking {
            manager.executeActiveMood(effectState(), skills, CharacterState(currentMp = 50, maxMp = 100))
        }
        assertTrue(cast.isEmpty())
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @Test fun saveAndLoad_roundtrips_activeMood() {
        val settings = com.russhwolf.settings.MapSettings()
        val p = Preferences(settings)
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)

        val mood = Mood("combat", listOf(
            MoodTrigger(100, "Butt-Rock Hair", 4055, "Disco Nap", 3),
            MoodTrigger(200, "Strength of the Grizzly", 4095, "Musk of the Moose", 1),
        ))
        manager.activeMood = mood
        manager.saveActiveMood()

        manager.activeMood = null           // clear in-memory
        manager.loadActiveMood()

        assertEquals(mood, manager.activeMood)
    }

    @Test fun saveAndLoad_emptyTriggerList_roundtrips() {
        val settings = com.russhwolf.settings.MapSettings()
        val p = Preferences(settings)
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)

        manager.activeMood = Mood("empty", emptyList())
        manager.saveActiveMood()
        manager.activeMood = null
        manager.loadActiveMood()

        assertEquals(Mood("empty", emptyList()), manager.activeMood)
    }

    @Test fun loadActiveMood_noPrefsData_setsNull() {
        val settings = com.russhwolf.settings.MapSettings()  // empty prefs
        val p = Preferences(settings)
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)
        manager.activeMood = Mood("x", emptyList())  // set something first

        manager.loadActiveMood()

        kotlin.test.assertNull(manager.activeMood)
    }

    @Test fun saveActiveMood_nullMood_clearsPrefs() {
        val settings = com.russhwolf.settings.MapSettings()
        settings.putString(Preferences.ACTIVE_MOOD_NAME, "old")
        settings.putString(Preferences.ACTIVE_MOOD_TRIGGERS, "100:EffectName:200:SkillName:1")
        val p = Preferences(settings)
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)

        manager.activeMood = null
        manager.saveActiveMood()

        assertEquals("", p.getString(Preferences.ACTIVE_MOOD_NAME))
        assertEquals("", p.getString(Preferences.ACTIVE_MOOD_TRIGGERS))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun skillData(
        id: Int,
        mpCost: Int = 0,
        dailyLimit: Int = 0,
        timesCast: Int = 0,
    ) = net.sourceforge.kolmafia.skill.SkillData(
        id = id, name = "Skill $id",
        type = net.sourceforge.kolmafia.skill.SkillType.PASSIVE,
        mpCost = mpCost, dailyLimit = dailyLimit, timesCast = timesCast,
    )

    /** Returns a fake SkillManager that records which skill IDs were cast. */
    private fun fakeCastSkillManager(cast: MutableList<Int>): SkillManager {
        val fakeClient = io.ktor.client.HttpClient(MockEngine { _ ->
            respond("")
        })
        val fakeRequest = net.sourceforge.kolmafia.skill.SkillCastRequest(fakeClient)
        val fakeEventBus = net.sourceforge.kolmafia.event.GameEventBus()
        return object : SkillManager(fakeClient, fakeRequest, fakeEventBus) {
            override suspend fun cast(
                skill: net.sourceforge.kolmafia.skill.SkillData,
                quantity: Int,
            ): Result<Unit> {
                cast.add(skill.id)
                return Result.success(Unit)
            }
        }
    }
}
