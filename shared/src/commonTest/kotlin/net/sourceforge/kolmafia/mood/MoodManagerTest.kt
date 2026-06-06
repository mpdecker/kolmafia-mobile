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
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    // ── removeMalignantEffects ────────────────────────────────────────────────

    @Test fun removeMalignantEffects_noMalignantEffects_doesNotCallUneffect() {
        val uneffected = mutableListOf<Int>()
        val manager = managerWithUneffect(uneffected, prefs())
        runBlocking {
            manager.removeMalignantEffects(effectState(effect(10, 3)))
        }
        assertTrue(uneffected.isEmpty())
    }

    @Test fun removeMalignantEffects_beatenUpPresent_callsUneffect() {
        val uneffected = mutableListOf<Int>()
        val manager = managerWithUneffect(uneffected, prefs())
        val beatenUp = EffectData(id = 4, name = "Beaten Up", duration = 5)
        runBlocking {
            manager.removeMalignantEffects(effectState(beatenUp))
        }
        assertEquals(listOf(4), uneffected)
    }

    @Test fun removeMalignantEffects_disabledByPref_doesNotUneffect() {
        val uneffected = mutableListOf<Int>()
        val s = MapSettings()
        s.putBoolean(Preferences.REMOVE_MALIGNANT_EFFECTS, false)
        val p = Preferences(s)
        val manager = managerWithUneffect(uneffected, p)
        val beatenUp = EffectData(id = 4, name = "Beaten Up", duration = 5)
        runBlocking {
            manager.removeMalignantEffects(effectState(beatenUp))
        }
        assertTrue(uneffected.isEmpty())
    }

    @Test fun removeMalignantEffects_multipleMalignantEffects_uneffectsAll() {
        val uneffected = mutableListOf<Int>()
        val manager = managerWithUneffect(uneffected, prefs())
        val effects = effectState(
            EffectData(id = 4,  name = "Beaten Up",              duration = 3),
            EffectData(id = 37, name = "Hardly Poisoned at All", duration = 1),
        )
        runBlocking { manager.removeMalignantEffects(effects) }
        assertEquals(setOf(4, 37), uneffected.toSet())
    }

    @Test fun executeActiveMood_clearsBeatenUp_evenWhenNoActiveMood() {
        val uneffected = mutableListOf<Int>()
        val manager = managerWithUneffect(uneffected, prefs())
        manager.activeMood = null
        val beatenUp = EffectData(id = 4, name = "Beaten Up", duration = 2)
        runBlocking {
            manager.executeActiveMood(effectState(beatenUp), SkillState(), CharacterState())
        }
        assertEquals(listOf(4), uneffected)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun managerWithUneffect(
        uneffected: MutableList<Int>,
        prefs: Preferences,
    ): MoodManager {
        val fakeUneffect = object : net.sourceforge.kolmafia.request.UneffectRequest(
            io.ktor.client.HttpClient(MockEngine { respond("") })
        ) {
            override suspend fun uneffect(effectId: Int): Result<Unit> {
                uneffected.add(effectId)
                return Result.success(Unit)
            }
        }
        return MoodManager(
            skillManager    = fakeCastSkillManager(mutableListOf()),
            preferences     = prefs,
            uneffectRequest = fakeUneffect,
        )
    }

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

    // ── Mood library ──────────────────────────────────────────────────────────

    @Test fun addMoodToLibrary_addsEntry() {
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
        val mood = Mood("farming", listOf(trigger(10, 200)))
        manager.addMoodToLibrary(mood)
        assertEquals(mood, manager.moodLibrary["farming"])
    }

    @Test fun addMoodToLibrary_upsertsByName() {
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
        manager.addMoodToLibrary(Mood("farming", listOf(trigger(10, 200))))
        val updated = Mood("farming", listOf(trigger(20, 300)))
        manager.addMoodToLibrary(updated)
        assertEquals(updated, manager.moodLibrary["farming"])
        assertEquals(1, manager.moodLibrary.size)
    }

    @Test fun removeMoodFromLibrary_removesEntry() {
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
        manager.addMoodToLibrary(Mood("farming", listOf(trigger(10, 200))))
        manager.removeMoodFromLibrary("farming")
        assertTrue(manager.moodLibrary.isEmpty())
    }

    @Test fun removeMoodFromLibrary_unknownName_doesNotCrash() {
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
        manager.removeMoodFromLibrary("nonexistent")  // should not throw
        assertTrue(manager.moodLibrary.isEmpty())
    }

    @Test fun setActiveMoodByName_knownName_setsActiveMoodAndReturnsTrue() {
        val s = MapSettings()
        val p = Preferences(s)
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)
        val mood = Mood("combat", listOf(trigger(10, 200)))
        manager.addMoodToLibrary(mood)
        val result = manager.setActiveMoodByName("combat")
        assertTrue(result)
        assertEquals(mood, manager.activeMood)
        // Also persists via saveActiveMood()
        assertEquals("combat", s.getString(Preferences.ACTIVE_MOOD_NAME, ""))
    }

    @Test fun setActiveMoodByName_unknownName_returnsFalse() {
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
        assertFalse(manager.setActiveMoodByName("doesNotExist"))
        assertNull(manager.activeMood)
    }

    @Test fun saveMoodLibrary_and_loadMoodLibrary_roundtrip() {
        val s = MapSettings()
        val p = Preferences(s)
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)
        manager.addMoodToLibrary(Mood("farming", listOf(trigger(10, 200, minTurns = 5))))
        manager.addMoodToLibrary(Mood("leveling", listOf(trigger(20, 300))))
        manager.saveMoodLibrary()

        // Fresh manager loads from same settings
        val manager2 = MoodManager(fakeCastSkillManager(mutableListOf()), p)
        manager2.loadMoodLibrary()
        assertEquals(2, manager2.moodLibrary.size)
        assertEquals(listOf(trigger(10, 200, minTurns = 5)), manager2.moodLibrary["farming"]?.triggers)
        assertEquals(listOf(trigger(20, 300)), manager2.moodLibrary["leveling"]?.triggers)
    }

    @Test fun loadMoodLibrary_emptyPrefs_setsEmptyMap() {
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
        manager.loadMoodLibrary()
        assertTrue(manager.moodLibrary.isEmpty())
    }

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
