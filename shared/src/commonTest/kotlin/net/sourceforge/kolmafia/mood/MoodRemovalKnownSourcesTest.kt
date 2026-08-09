package net.sourceforge.kolmafia.mood

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MoodRemovalKnownSourcesTest {

    @BeforeTest
    @AfterTest
    fun resetRegistry() {
        MoodRemovalKnownSources.clear()
    }

    @Test
    fun register_skipsBlankAction() {
        MoodRemovalKnownSources.register("Sleepy", "")
        assertEquals("", MoodRemovalKnownSources.getKnownSources("Sleepy"))
    }

    @Test
    fun getKnownSources_pipeJoinsInRegistrationOrder() {
        MoodRemovalKnownSources.register("Sleepy", "cast Disco Nap")
        MoodRemovalKnownSources.register("Sleepy", "use 1 decorative fountain")
        assertEquals(
            "cast Disco Nap|use 1 decorative fountain",
            MoodRemovalKnownSources.getKnownSources("Sleepy"),
        )
    }

    @Test
    fun rebuildFromLibrary_scansLoseEffectRemovalTriggers() {
        val mood = Mood(
            "run",
            removalTriggers = listOf(
                MoodRemovalTrigger(
                    type = MoodRemovalTriggerType.LOSE_EFFECT,
                    effectId = 2,
                    effectName = "Sleepy",
                    action = "cast Disco Nap",
                ),
                MoodRemovalTrigger(
                    type = MoodRemovalTriggerType.GAIN_EFFECT,
                    effectId = 7,
                    effectName = "Beaten Up",
                    action = "hottub",
                ),
            ),
        )
        MoodRemovalKnownSources.rebuildFromLibrary(listOf(mood))
        assertEquals("cast Disco Nap", MoodRemovalKnownSources.getKnownSources("Sleepy"))
        assertEquals("", MoodRemovalKnownSources.getKnownSources("Beaten Up"))
    }

    @Test
    fun rebuildFromLibrary_clearsPreviousEntries() {
        MoodRemovalKnownSources.register("Sleepy", "stale action")
        MoodRemovalKnownSources.rebuildFromLibrary(emptyList())
        assertEquals("", MoodRemovalKnownSources.getKnownSources("Sleepy"))
    }
}
