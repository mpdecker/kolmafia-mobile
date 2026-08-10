package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import net.sourceforge.kolmafia.skill.SkillType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoodRemovalTriggerExecutionTest {

    @BeforeTest
    fun setUp() {
        UneffectSkillEffectMap.resetForTest()
        runBlocking {
            EffectDatabase.load()
            SkillDefinitionDatabase.load()
        }
        UneffectSkillEffectMap.rebuild()
    }

    @AfterTest
    fun tearDown() {
        UneffectSkillEffectMap.resetForTest()
        SkillDefinitionDatabase.resetForTest()
        EffectDatabase.resetForTest()
    }

    private fun effect(id: Int, name: String, duration: Int) =
        EffectData(id = id, name = name, duration = duration)

    private fun effectState(vararg effects: EffectData) =
        EffectState(effects = effects.toList())

    private fun loseTrigger(effectId: Int, effectName: String, action: String) =
        MoodRemovalTrigger(
            type = MoodRemovalTriggerType.LOSE_EFFECT,
            effectId = effectId,
            effectName = effectName,
            action = action,
        )

    private fun defaultCharState() = CharacterState()

    private fun defaultPrefs() = Preferences(MapSettings())

    @Test fun shouldExecute_unconditional_alwaysTrue() {
        val trigger = MoodRemovalTrigger(
            type = MoodRemovalTriggerType.UNCONDITIONAL,
            effectId = 0,
            effectName = "",
            action = "visit clan",
        )
        assertTrue(
            MoodRemovalTriggerExecution.shouldExecute(
                trigger, effectState(), defaultCharState(), defaultPrefs(),
            ),
        )
    }

    @Test fun shouldExecute_gainEffect_requiresActiveEffect() {
        val trigger = MoodRemovalTrigger(
            type = MoodRemovalTriggerType.GAIN_EFFECT,
            effectId = 4,
            effectName = "Beaten Up",
            action = "use 1 [829]",
        )
        assertFalse(
            MoodRemovalTriggerExecution.shouldExecute(
                trigger, effectState(), defaultCharState(), defaultPrefs(),
            ),
        )
        assertTrue(
            MoodRemovalTriggerExecution.shouldExecute(
                trigger,
                effectState(effect(4, "Beaten Up", 5)),
                defaultCharState(),
                defaultPrefs(),
            ),
        )
    }

    @Test fun shouldExecute_loseEffect_firesWhenDurationAtMostFive() {
        val trigger = loseTrigger(100, "Mighty", "cast Disco Nap")
        assertTrue(
            MoodRemovalTriggerExecution.shouldExecute(
                trigger,
                effectState(effect(100, "Mighty", 5)),
                defaultCharState(),
                defaultPrefs(),
            ),
        )
        assertFalse(
            MoodRemovalTriggerExecution.shouldExecute(
                trigger,
                effectState(effect(100, "Mighty", 6)),
                defaultCharState(),
                defaultPrefs(),
            ),
        )
    }

    @Test fun shouldExecute_loseEffect_unstackableAction_requiresAbsentEffect() {
        val trigger = loseTrigger(200, "Absinthe Minded", "use 1 absinthe")
        assertTrue(
            MoodRemovalTriggerExecution.shouldExecute(
                trigger, effectState(), defaultCharState(), defaultPrefs(),
            ),
        )
        assertFalse(
            MoodRemovalTriggerExecution.shouldExecute(
                trigger,
                effectState(effect(200, "Absinthe Minded", 3)),
                defaultCharState(),
                defaultPrefs(),
            ),
        )
    }

    @Test fun shouldExecute_loseEffect_multiplicityAlwaysTrue() {
        val trigger = loseTrigger(100, "Mighty", "cast Disco Nap")
        assertTrue(
            MoodRemovalTriggerExecution.shouldExecute(
                trigger,
                effectState(effect(100, "Mighty", 50)),
                defaultCharState(),
                defaultPrefs(),
                multiplicity = 1,
            ),
        )
    }

    @Test fun sortedForExecution_ordersByDesktopTypePriority() {
        val unconditional = MoodRemovalTrigger(
            MoodRemovalTriggerType.UNCONDITIONAL, 0, "", "visit clan",
        )
        val gain = MoodRemovalTrigger(
            MoodRemovalTriggerType.GAIN_EFFECT, 4, "Beaten Up", "use 1 [829]",
        )
        val lose = loseTrigger(100, "Mighty", "cast Disco Nap")
        val sorted = MoodRemovalTriggerExecution.sortedForExecution(listOf(lose, gain, unconditional))
        assertEquals(listOf(unconditional, gain, lose), sorted)
    }

    @Test fun executeActiveMood_loseEffectCastWhenLow_executesSkill() = runBlocking {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), Preferences(MapSettings()))
        manager.activeMood = Mood(
            "run",
            removalTriggers = listOf(
                loseTrigger(100, "Mighty", "cast Disco Nap"),
            ),
        )
        val skills = SkillState(
            skills = listOf(
                skillData(id = 4055, name = "Disco Nap", mpCost = 10),
            ),
        )
        manager.executeActiveMood(
            effectState(effect(100, "Mighty", 3)),
            skills,
            CharacterState(currentMp = 50, maxMp = 100),
        )
        assertEquals(listOf(4055), cast)
    }

    @Test fun executeActiveMood_loseEffectCastWhenHigh_skipsSkill() = runBlocking {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), Preferences(MapSettings()))
        manager.activeMood = Mood(
            "run",
            removalTriggers = listOf(
                loseTrigger(100, "Mighty", "cast Disco Nap"),
            ),
        )
        val skills = SkillState(
            skills = listOf(
                skillData(id = 4055, name = "Disco Nap", mpCost = 10),
            ),
        )
        manager.executeActiveMood(
            effectState(effect(100, "Mighty", 10)),
            skills,
            CharacterState(currentMp = 50, maxMp = 100),
        )
        assertTrue(cast.isEmpty())
    }

    @Test fun scaledCount_matchesDesktopFormula() {
        assertEquals(1, MoodRemovalTriggerExecution.scaledCount(1, 0))
        assertEquals(2, MoodRemovalTriggerExecution.scaledCount(2, 0))
        assertEquals(2, MoodRemovalTriggerExecution.scaledCount(1, 2))
        assertEquals(6, MoodRemovalTriggerExecution.scaledCount(2, 3))
    }

    @Test fun parseCastCount_readsNumericPrefix() {
        assertEquals(2, net.sourceforge.kolmafia.request.MoodUneffectActionParser.parseCastCount("cast 2 Disco Nap"))
        assertEquals(1, net.sourceforge.kolmafia.request.MoodUneffectActionParser.parseCastCount("cast Disco Nap"))
    }

    @Test fun executeActiveMood_loseEffectCastScalesWithMultiplicity() = runBlocking {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), Preferences(MapSettings()))
        manager.activeMood = Mood(
            "run",
            removalTriggers = listOf(
                loseTrigger(100, "Mighty", "cast 2 Disco Nap"),
            ),
        )
        val skills = SkillState(
            skills = listOf(
                skillData(id = 4055, name = "Disco Nap", mpCost = 10),
            ),
        )
        manager.executeActiveMood(
            effectState(effect(100, "Mighty", 3)),
            skills,
            CharacterState(currentMp = 500, maxMp = 500),
            multiplicity = 3,
        )
        assertEquals(List(6) { 4055 }, cast)
    }

    @Test fun executeActiveMood_loseEffectHighDuration_executesWhenMultiplicityPositive() = runBlocking {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), Preferences(MapSettings()))
        manager.activeMood = Mood(
            "run",
            removalTriggers = listOf(
                loseTrigger(100, "Mighty", "cast Disco Nap"),
            ),
        )
        val skills = SkillState(
            skills = listOf(
                skillData(id = 4055, name = "Disco Nap", mpCost = 10),
            ),
        )
        manager.executeActiveMood(
            effectState(effect(100, "Mighty", 10)),
            skills,
            CharacterState(currentMp = 50, maxMp = 100),
            multiplicity = 1,
        )
        assertEquals(listOf(4055), cast)
    }

    @Test fun executeActiveMood_gainEffect_invokesCliExecutor() = runBlocking {
        val commands = mutableListOf<String>()
        val manager = MoodManager(fakeCastSkillManager(mutableListOf()), Preferences(MapSettings()))
        manager.cliExecutor = { commands.add(it) }
        manager.activeMood = Mood(
            "run",
            removalTriggers = listOf(
                MoodRemovalTrigger(
                    type = MoodRemovalTriggerType.GAIN_EFFECT,
                    effectId = 4,
                    effectName = "Beaten Up",
                    action = "hottub",
                ),
            ),
        )
        manager.executeActiveMood(
            effectState(effect(4, "Beaten Up", 2)),
            SkillState(),
            CharacterState(),
        )
        assertEquals(listOf("hottub"), commands)
    }

    @Test fun isEffectMappedSkillTrigger_trueWhenEffectMapsToSkill() {
        val trigger = loseTrigger(601, "Fat Leon's Phat Loot Lyric", "cast Fat Leon's Phat Loot Lyric")
        assertTrue(MoodRemovalTriggerExecution.isEffectMappedSkillTrigger(trigger))
    }

    private fun skillData(
        id: Int,
        name: String,
        mpCost: Int = 0,
        dailyLimit: Int = 0,
        timesCast: Int = 0,
    ) = net.sourceforge.kolmafia.skill.SkillData(
        id = id,
        name = name,
        type = SkillType.PASSIVE,
        mpCost = mpCost,
        dailyLimit = dailyLimit,
        timesCast = timesCast,
    )

    private fun fakeCastSkillManager(cast: MutableList<Int>): SkillManager {
        val fakeClient = io.ktor.client.HttpClient(MockEngine { respond("") })
        val fakeRequest = net.sourceforge.kolmafia.skill.SkillCastRequest(fakeClient)
        val fakeEventBus = net.sourceforge.kolmafia.event.GameEventBus()
        return object : SkillManager(fakeClient, fakeRequest, fakeEventBus) {
            override suspend fun cast(
                skill: net.sourceforge.kolmafia.skill.SkillData,
                quantity: Int,
            ): Result<Unit> {
                repeat(quantity) { cast.add(skill.id) }
                return Result.success(Unit)
            }
        }
    }
}
