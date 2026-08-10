package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import net.sourceforge.kolmafia.skill.SkillType

class MoodAutofillTest {

    @BeforeTest
    fun setUp() {
        MoodRemovalKnownSources.clear()
        UneffectSkillEffectMap.resetForTest()
        runBlocking {
            EffectDatabase.load()
            SkillDefinitionDatabase.load()
        }
        UneffectSkillEffectMap.rebuild()
    }

    @AfterTest
    fun tearDown() {
        MoodRemovalKnownSources.clear()
        UneffectSkillEffectMap.resetForTest()
    }

    private fun manager(): MoodManager =
        MoodManager(fakeSkillManager(), Preferences(MapSettings()))

    private fun fakeSkillManager(): SkillManager {
        val client = HttpClient(MockEngine { respond("") })
        return SkillManager(client, SkillCastRequest(client), GameEventBus())
    }

    @Test
    fun minimalSet_addsLoseEffectTriggerForActiveEffectWithDefaultAction() {
        val mgr = manager()
        mgr.addMoodToLibrary(Mood("run"))
        mgr.activeMood = mgr.moodLibrary["run"]

        val effectState = EffectState(
            effects = listOf(
                net.sourceforge.kolmafia.effect.EffectData(id = 2, name = "Sleepy", duration = 5),
            ),
        )
        mgr.minimalSet(effectState)

        val trigger = mgr.activeMood?.removalTriggers?.single()
        assertEquals(MoodRemovalTriggerType.LOSE_EFFECT, trigger?.type)
        assertEquals("Sleepy", trigger?.effectName)
        assertEquals("use 1 decorative fountain", trigger?.action)
    }

    @Test
    fun minimalSet_noOpWhenApathetic() {
        val mgr = manager()
        mgr.activeMood = Mood("apathetic")
        mgr.minimalSet(
            EffectState(
                effects = listOf(
                    net.sourceforge.kolmafia.effect.EffectData(id = 2, name = "Sleepy", duration = 5),
                ),
            ),
        )
        assertTrue(mgr.activeMood?.removalTriggers.isNullOrEmpty())
    }

    @Test
    fun minimalSet_noOpWhenNoActiveMood() {
        val mgr = manager()
        mgr.minimalSet(
            EffectState(
                effects = listOf(
                    net.sourceforge.kolmafia.effect.EffectData(id = 2, name = "Sleepy", duration = 5),
                ),
            ),
        )
        assertEquals(null, mgr.activeMood)
    }

    @Test
    fun addActiveLoseEffectTrigger_replacesDuplicateEffectName() {
        val mgr = manager()
        mgr.activeMood = Mood(
            "run",
            removalTriggers = listOf(
                MoodRemovalTrigger(
                    type = MoodRemovalTriggerType.LOSE_EFFECT,
                    effectId = 2,
                    effectName = "Sleepy",
                    action = "cast Disco Nap",
                ),
            ),
        )

        assertTrue(mgr.addActiveLoseEffectTrigger("Sleepy", "use 1 decorative fountain"))
        assertEquals(1, mgr.activeMood?.removalTriggers?.size)
        assertEquals("use 1 decorative fountain", mgr.activeMood?.removalTriggers?.single()?.action)
    }

    @Test
    fun maximalSet_addsRankedAtSongTriggers() {
        val mgr = manager()
        mgr.addMoodToLibrary(Mood("run"))
        mgr.activeMood = mgr.moodLibrary["run"]

        val skillState = SkillState(
            skills = listOf(
                SkillData(id = 6010, name = "Fat Leon's Phat Loot Lyric", type = SkillType.NONCOMBAT, mpCost = 11, dailyLimit = 0, timesCast = 0),
                SkillData(id = 6011, name = "The Moxious Madrigal", type = SkillType.NONCOMBAT, mpCost = 11, dailyLimit = 0, timesCast = 0),
                SkillData(id = 6012, name = "Aloysius' Antiphon of Aptitude", type = SkillType.NONCOMBAT, mpCost = 11, dailyLimit = 0, timesCast = 0),
                SkillData(id = 6013, name = "The Sonata of Sneakiness", type = SkillType.NONCOMBAT, mpCost = 11, dailyLimit = 0, timesCast = 0),
            ),
        )
        val charState = CharacterState(
            characterClass = CharacterClass.ACCORDION_THIEF.id,
            isHardcore = false,
        )

        mgr.maximalSet(EffectState(), skillState, charState)

        val loseTriggers = mgr.activeMood?.removalTriggers.orEmpty()
        assertEquals(3, loseTriggers.size)
        assertTrue(SkillDefinitionProxy.isAccordionThiefSong(6010))
        assertEquals(
            setOf(
                "cast Fat Leon's Phat Loot Lyric",
                "cast Aloysius' Antiphon of Aptitude",
                "cast The Sonata of Sneakiness",
            ),
            loseTriggers.map { it.action }.toSet(),
        )
    }

    @Test
    fun loadActiveMood_mergesRemovalTriggersFromLibrary() {
        val prefs = Preferences(MapSettings())
        val mgr = MoodManager(fakeSkillManager(), prefs)
        val removal = MoodRemovalTrigger(
            type = MoodRemovalTriggerType.LOSE_EFFECT,
            effectId = 2,
            effectName = "Sleepy",
            action = "cast Disco Nap",
        )
        mgr.addMoodToLibrary(Mood("run", removalTriggers = listOf(removal)))
        mgr.saveMoodLibrary()
        prefs.setString(Preferences.ACTIVE_MOOD_NAME, "run")
        prefs.setString(Preferences.ACTIVE_MOOD_TRIGGERS, "")

        val mgr2 = MoodManager(fakeSkillManager(), prefs)
        mgr2.loadMoodLibrary()
        mgr2.loadActiveMood()

        assertEquals("cast Disco Nap", mgr2.activeMood?.removalTriggers?.single()?.action)
    }

    @Test
    fun skillToEffect_reverseLookupFromCastAction() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 9_000_001,
                name = "Test Buff Effect",
                image = "test.gif",
                descId = "test",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "cast 1 Test Cast Skill",
            ),
        )
        UneffectSkillEffectMap.rebuild()
        assertEquals("Test Buff Effect", UneffectSkillEffectMap.skillToEffect("Test Cast Skill"))
        assertEquals("Test Cast Skill", UneffectSkillEffectMap.effectToSkill("Test Buff Effect"))
    }

    @Test
    fun isAccordionThiefSong_matchesDesktopRange() {
        assertTrue(SkillDefinitionProxy.isAccordionThiefSong(6010))
        assertFalse(SkillDefinitionProxy.isAccordionThiefSong(6025))
        assertFalse(SkillDefinitionProxy.isAccordionThiefSong(5001))
    }
}
