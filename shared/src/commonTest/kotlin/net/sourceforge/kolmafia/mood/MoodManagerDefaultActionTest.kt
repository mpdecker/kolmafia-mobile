package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UneffectRemovableMaps
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager

class MoodManagerDefaultActionTest {

    @BeforeTest
    fun loadEffects() {
        MoodRemovalKnownSources.clear()
        runBlocking { EffectDatabase.load() }
    }

    @AfterTest
    fun resetKnownSources() {
        MoodRemovalKnownSources.clear()
    }

    private fun manager(): MoodManager =
        MoodManager(fakeSkillManager(), Preferences(MapSettings()))

    private fun fakeSkillManager(): SkillManager {
        val client = HttpClient(MockEngine { respond("") })
        return SkillManager(client, SkillCastRequest(client), GameEventBus())
    }

    @Test
    fun getDefaultAction_activeMoodGainEffectTrigger_returnsAction() {
        val mgr = manager()
        val trigger = MoodRemovalTrigger(
            type = MoodRemovalTriggerType.GAIN_EFFECT,
            effectId = 7,
            effectName = "Beaten Up",
            action = "use 1 [829]",
        )
        val mood = Mood("run", removalTriggers = listOf(trigger))
        mgr.addMoodToLibrary(mood)
        mgr.activeMood = mood

        assertEquals("use 1 [829]", mgr.getDefaultAction("gain_effect", "Beaten Up"))
    }

    @Test
    fun getDefaultAction_castWithoutSkill_clearsInResolverOnly() {
        val mgr = manager()
        val trigger = MoodRemovalTrigger(
            type = MoodRemovalTriggerType.GAIN_EFFECT,
            effectId = 7,
            effectName = "Beaten Up",
            action = "cast Shake It Off",
        )
        val mood = Mood("run", removalTriggers = listOf(trigger))
        mgr.activeMood = mood

        assertEquals("cast Shake It Off", mgr.getDefaultAction("gain_effect", "Beaten Up"))
    }

    @Test
    fun getDefaultAction_noTriggerRemovableEffect_returnsUneffectName() {
        val mgr = manager()
        mgr.activeMood = Mood("run")

        val action = mgr.getDefaultAction("gain_effect", "Beaten Up")
        assertEquals("uneffect Beaten Up", action)
    }

    @Test
    fun getDefaultAction_unremovableEffect_returnsEmpty() {
        val mgr = manager()
        mgr.activeMood = Mood("run")

        assertEquals("", mgr.getDefaultAction("gain_effect", "Goofball Withdrawal"))
        assertFalse(UneffectRemovableMaps.isRemovable(111))
    }

    @Test
    fun addRemovalTrigger_validLine_addsToLibrary() {
        val mgr = manager()
        mgr.addMoodToLibrary(Mood("run"))
        assertTrue(mgr.addRemovalTrigger("run", "gain_effect", "Beaten Up", "hottub"))
        assertEquals("hottub", mgr.moodLibrary["run"]?.removalTriggers?.single()?.action)
    }

    @Test
    fun saveAndLoadMoodLibrary_roundTripsRemovalTriggers() {
        val prefs = Preferences(MapSettings())
        val mgr = MoodManager(fakeSkillManager(), prefs)
        val trigger = MoodRemovalTrigger(
            type = MoodRemovalTriggerType.GAIN_EFFECT,
            effectId = 8,
            effectName = "Hardly Poisoned At All",
            action = "use 1 [829]",
        )
        mgr.addMoodToLibrary(Mood("run", removalTriggers = listOf(trigger)))
        mgr.saveMoodLibrary()

        val mgr2 = MoodManager(fakeSkillManager(), prefs)
        mgr2.loadMoodLibrary()
        val loaded = mgr2.moodLibrary["run"]?.removalTriggers?.single()
        assertEquals(MoodRemovalTriggerType.GAIN_EFFECT, loaded?.type)
        assertEquals("use 1 [829]", loaded?.action)
    }

    @Test
    fun getDefaultAction_activeMoodLoseEffectTrigger_returnsAction() {
        val mgr = manager()
        val trigger = MoodRemovalTrigger(
            type = MoodRemovalTriggerType.LOSE_EFFECT,
            effectId = 2,
            effectName = "Sleepy",
            action = "cast Disco Nap",
        )
        val mood = Mood("run", removalTriggers = listOf(trigger))
        mgr.addMoodToLibrary(mood)
        mgr.activeMood = mood

        assertEquals("cast Disco Nap", mgr.getDefaultAction("lose_effect", "Sleepy"))
    }

    @Test
    fun getDefaultAction_loseEffectNoTrigger_returnsStatuseffectsDefault() {
        val mgr = manager()
        mgr.activeMood = Mood("run")

        assertEquals("use 1 decorative fountain", mgr.getDefaultAction("lose_effect", "Sleepy"))
    }

    @Test
    fun getDefaultAction_loseEffectFallsThroughToKnownSources() {
        val mgr = manager()
        mgr.addMoodToLibrary(
            Mood(
                "other",
                removalTriggers = listOf(
                    MoodRemovalTrigger(
                        type = MoodRemovalTriggerType.LOSE_EFFECT,
                        effectId = 59,
                        effectName = "Wanged",
                        action = "cast Disco Nap",
                    ),
                ),
            ),
        )
        mgr.activeMood = Mood("run")

        assertEquals("cast Disco Nap", mgr.getDefaultAction("lose_effect", "Wanged"))
    }

    @Test
    fun getDefaultAction_loseEffectNoteOnlyAction_fallsThroughToKnownSources() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 59,
                name = "Wanged",
                image = "wang.gif",
                descId = "test",
                quality = EffectQuality.NEUTRAL,
                attributes = emptySet(),
                actions = "# wang used on you",
            ),
        )
        val mgr = manager()
        mgr.addMoodToLibrary(
            Mood(
                "other",
                removalTriggers = listOf(
                    MoodRemovalTrigger(
                        type = MoodRemovalTriggerType.LOSE_EFFECT,
                        effectId = 59,
                        effectName = "Wanged",
                        action = "use 1 [829]",
                    ),
                ),
            ),
        )
        mgr.activeMood = Mood("run")

        assertEquals("use 1 [829]", mgr.getDefaultAction("lose_effect", "Wanged"))
    }

    @Test
    fun loadMoodLibrary_rebuildsKnownSourcesFromLoseEffectTriggers() {
        val prefs = Preferences(MapSettings())
        val mgr = MoodManager(fakeSkillManager(), prefs)
        mgr.addMoodToLibrary(
            Mood(
                "run",
                removalTriggers = listOf(
                    MoodRemovalTrigger(
                        type = MoodRemovalTriggerType.LOSE_EFFECT,
                        effectId = 2,
                        effectName = "Sleepy",
                        action = "cast Disco Nap",
                    ),
                ),
            ),
        )
        mgr.saveMoodLibrary()
        MoodRemovalKnownSources.clear()

        val mgr2 = MoodManager(fakeSkillManager(), prefs)
        mgr2.loadMoodLibrary()

        assertEquals("cast Disco Nap", MoodRemovalKnownSources.getKnownSources("Sleepy"))
        assertEquals("use 1 decorative fountain", mgr2.getDefaultAction("lose_effect", "Sleepy"))
    }
}
