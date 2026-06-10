package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoodInheritanceTest {

    private fun trigger(effectId: Int, skillId: Int, minTurns: Int = 1) =
        MoodTrigger(effectId, "Effect $effectId", skillId, "Skill $skillId", minTurns)

    private fun prefs(): Preferences = Preferences(MapSettings().apply {
        putBoolean(Preferences.AUTO_BUFF, true)
    })

    // ── parseName ────────────────────────────────────────────────────────────

    @Test fun parseName_extendsClause_splitsNameAndParents() {
        val (name, parents) = Mood.parseName("run extends default")
        assertEquals("run", name)
        assertEquals(listOf("default"), parents)
    }

    @Test fun parseName_extendsClause_isCaseInsensitive() {
        val (name, parents) = Mood.parseName("Run EXTENDS Default")
        assertEquals("run", name)
        assertEquals(listOf("default"), parents)
    }

    @Test fun parseName_multiParent_extendsClause() {
        val (name, parents) = Mood.parseName("run extends default, farming")
        assertEquals("run", name)
        assertEquals(listOf("default", "farming"), parents)
    }

    @Test fun parseName_commaOnlyParents_emptyName() {
        val (name, parents) = Mood.parseName("default, farming")
        assertEquals("", name)
        assertEquals(listOf("default", "farming"), parents)
    }

    @Test fun parseName_normalizesWhitespaceAndCase() {
        val (name, _) = Mood.parseName("My Mood")
        assertEquals("mymood", name)
    }

    @Test fun parseName_clearMapsToDefault() {
        val (name, _) = Mood.parseName("clear")
        assertEquals("default", name)
    }

    @Test fun displayName_roundTripsExtendsClause() {
        val mood = Mood("run", emptyList(), parentNames = listOf("default"))
        assertEquals("run extends default", mood.displayName())
        assertEquals(mood, Mood.parseName(mood.displayName()).let { (n, p) ->
            Mood(n, emptyList(), p)
        }.copy(triggers = mood.triggers))
    }

    // ── effectiveTriggers ────────────────────────────────────────────────────

    @Test fun effectiveTriggers_inheritsParentTriggers() {
        val default = Mood("default", triggers = listOf(trigger(10, 200)))
        val run = Mood("run", emptyList(), parentNames = listOf("default"))
        val library = mapOf("default" to default, "run" to run)
        assertEquals(listOf(trigger(10, 200)), run.effectiveTriggers(library))
    }

    @Test fun effectiveTriggers_childOverridesParentDuplicate() {
        val parentTrigger = trigger(10, 200, minTurns = 1)
        val childTrigger = trigger(10, 300, minTurns = 5)
        val default = Mood("default", triggers = listOf(parentTrigger))
        val run = Mood("run", listOf(childTrigger), parentNames = listOf("default"))
        val library = mapOf("default" to default, "run" to run)
        assertEquals(listOf(childTrigger), run.effectiveTriggers(library))
    }

    @Test fun effectiveTriggers_multiParentMergeOrder() {
        val t1 = trigger(10, 200)
        val t2 = trigger(20, 300)
        val t3 = trigger(30, 400)
        val a = Mood("a", triggers = listOf(t1))
        val b = Mood("b", triggers = listOf(t2))
        val child = Mood("child", listOf(t3), parentNames = listOf("a", "b"))
        val library = mapOf("a" to a, "b" to b, "child" to child)
        assertEquals(listOf(t1, t2, t3), child.effectiveTriggers(library))
    }

    @Test fun effectiveTriggers_cycleDetection_avoidsInfiniteRecursion() {
        val a = Mood("a", listOf(trigger(10, 200)), parentNames = listOf("b"))
        val b = Mood("b", listOf(trigger(20, 300)), parentNames = listOf("a"))
        val library = mapOf("a" to a, "b" to b)
        val merged = a.effectiveTriggers(library)
        assertEquals(2, merged.size)
        assertTrue(merged.any { it.effectId == 10 })
        assertTrue(merged.any { it.effectId == 20 })
    }

    // ── missingTriggers with inheritance ─────────────────────────────────────

    @Test fun missingTriggers_seesInheritedTriggers() {
        val default = Mood("default", triggers = listOf(trigger(10, 200)))
        val run = Mood("run", emptyList(), parentNames = listOf("default"))
        val library = mapOf("default" to default, "run" to run)
        val missing = MoodManager.missingTriggers(run, EffectState(), library)
        assertEquals(listOf(trigger(10, 200)), missing)
    }

    // ── executeActiveMood with inheritance ───────────────────────────────────

    @Test fun executeActiveMood_castsInheritedSkill() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.addMoodToLibrary(Mood("default", triggers = listOf(trigger(10, 200))))
        manager.addMoodToLibrary(Mood("run", emptyList(), parentNames = listOf("default")))
        manager.activeMood = manager.moodLibrary["run"]
        runBlocking {
            manager.executeActiveMood(
                EffectState(),
                SkillState(skills = listOf(
                    SkillData(200, "Skill 200", SkillType.COMBAT, mpCost = 5, dailyLimit = 0, timesCast = 0),
                )),
                CharacterState(currentMp = 50, maxMp = 100),
            )
        }
        assertEquals(listOf(200), cast)
    }

    // ── setActiveMoodByName with extends display string ──────────────────────

    @Test fun setActiveMoodByName_parsesExtendsDisplayString() {
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
        manager.addMoodToLibrary(Mood("default", triggers = listOf(trigger(10, 200))))
        manager.addMoodToLibrary(Mood("run", emptyList(), parentNames = listOf("default")))
        assertTrue(manager.setActiveMoodByName("run extends default"))
        assertEquals("run", manager.activeMood?.name)
        assertEquals(listOf("default"), manager.activeMood?.parentNames)
    }

    // ── persistence round-trip ───────────────────────────────────────────────

    @Test fun saveAndLoadLibrary_roundtripsInheritance() {
        val settings = MapSettings()
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), Preferences(settings))
        manager.addMoodToLibrary(Mood("default", triggers = listOf(trigger(10, 200))))
        manager.addMoodToLibrary(Mood("run", listOf(trigger(20, 300)), parentNames = listOf("default")))
        manager.saveMoodLibrary()

        val manager2 = MoodManager(fakeCastSkillManager(mutableListOf()), Preferences(settings))
        manager2.loadMoodLibrary()

        assertEquals("run extends default", manager2.moodLibrary["run"]?.displayName())
        assertEquals(listOf("default"), manager2.moodLibrary["run"]?.parentNames)
        assertEquals(listOf(trigger(20, 300)), manager2.moodLibrary["run"]?.triggers)
    }

    @Test fun saveAndLoadActiveMood_roundtripsInheritance() {
        val settings = MapSettings()
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), Preferences(settings))
        manager.activeMood = Mood("run", listOf(trigger(20, 300)), parentNames = listOf("default"))
        manager.saveActiveMood()

        val manager2 = MoodManager(fakeCastSkillManager(mutableListOf()), Preferences(settings))
        manager2.loadActiveMood()

        assertEquals("run", manager2.activeMood?.name)
        assertEquals(listOf("default"), manager2.activeMood?.parentNames)
        assertEquals("run extends default", settings.getString(Preferences.ACTIVE_MOOD_NAME, ""))
    }

    private fun fakeCastSkillManager(captured: MutableList<Int>): SkillManager {
        val engine = MockEngine { respond("OK") }
        val fakeRequest = net.sourceforge.kolmafia.skill.SkillCastRequest(io.ktor.client.HttpClient(engine))
        val fakeEventBus = net.sourceforge.kolmafia.event.GameEventBus()
        return object : SkillManager(io.ktor.client.HttpClient(engine), fakeRequest, fakeEventBus) {
            override suspend fun cast(
                skill: SkillData,
                quantity: Int,
            ): Result<Unit> {
                repeat(quantity) { captured += skill.id }
                return Result.success(Unit)
            }
        }
    }
}
