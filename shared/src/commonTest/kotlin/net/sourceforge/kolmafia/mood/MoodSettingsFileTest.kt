package net.sourceforge.kolmafia.mood

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap

class MoodSettingsFileTest {

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

    @Test
    fun parse_extendsHeaderAndGainEffect() {
        val text = """
            [ combat extends default ]
            gain_effect Beaten Up => uneffect Beaten Up
        """.trimIndent()

        val moods = MoodSettingsFile.parse(text)
        val combat = moods["combat"] ?: error("missing combat mood")
        assertEquals(listOf("default"), combat.parentNames)
        assertEquals(1, combat.removalTriggers.size)
        assertEquals(MoodRemovalTriggerType.GAIN_EFFECT, combat.removalTriggers.single().type)
    }

    @Test
    fun parse_seedsApatheticAndDefault() {
        val moods = MoodSettingsFile.parse("")
        assertTrue(moods.containsKey("apathetic"))
        assertTrue(moods.containsKey("default"))
    }

    @Test
    fun parse_castLoseEffectDerivesBuffTrigger() {
        val text = """
            [ run ]
            lose_effect Fat Leon's Phat Loot Lyric => cast Fat Leon's Phat Loot Lyric
        """.trimIndent()

        val run = MoodSettingsFile.parse(text)["run"] ?: error("missing run mood")
        assertEquals(1, run.removalTriggers.size)
        assertEquals(1, run.triggers.size)
        assertEquals(5, run.triggers.single().minimumTurns)
    }

    @Test
    fun roundTrip_preservesMoodSections() {
        val original = Mood(
            name = "run",
            triggers = emptyList(),
            parentNames = listOf("default"),
            removalTriggers = listOf(
                MoodRemovalTrigger(
                    type = MoodRemovalTriggerType.UNCONDITIONAL,
                    effectId = 0,
                    effectName = "",
                    action = "restore mp",
                ),
            ),
        )
        val serialized = MoodSettingsFile.serialize(listOf(original))
        val parsed = MoodSettingsFile.parse(serialized)["run"] ?: error("missing run mood")
        assertEquals(listOf("default"), parsed.parentNames)
        assertEquals("restore mp", parsed.removalTriggers.single().action)
    }
}
