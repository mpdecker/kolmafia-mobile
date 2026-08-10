package net.sourceforge.kolmafia.mood

import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.data.EffectDatabase

class MoodRemovalTriggerParserTest {

    @BeforeTest
    fun loadEffects() {
        runBlocking { EffectDatabase.load() }
    }

    @Test
    fun parseLine_gainEffectUseItem_parsesTrigger() {
        val trigger = MoodRemovalTriggerParser.parseLine("gain_effect Beaten Up => use 1 [829]")
        assertEquals(MoodRemovalTriggerType.GAIN_EFFECT, trigger?.type)
        assertEquals(7, trigger?.effectId)
        assertEquals("Beaten Up", trigger?.effectName)
        assertEquals("use 1 [829]", trigger?.action)
    }

    @Test
    fun parseLine_loseEffectCast_parsesTrigger() {
        val trigger = MoodRemovalTriggerParser.parseLine("lose_effect Confused => cast Disco Nap")
        assertEquals(MoodRemovalTriggerType.LOSE_EFFECT, trigger?.type)
        assertEquals(3, trigger?.effectId)
        assertEquals("Confused", trigger?.effectName)
        assertEquals("cast Disco Nap", trigger?.action)
    }

    @Test
    fun parseLine_unconditional_parsesTrigger() {
        val trigger = MoodRemovalTriggerParser.parseLine("unconditional => visit clan")
        assertEquals(MoodRemovalTriggerType.UNCONDITIONAL, trigger?.type)
        assertEquals("", trigger?.effectName)
        assertEquals("visit clan", trigger?.action)
    }

    @Test
    fun parseLine_missingArrow_returnsNull() {
        assertNull(MoodRemovalTriggerParser.parseLine("gain_effect Beaten Up use 1 [829]"))
    }

    @Test
    fun parseLine_unknownEffect_returnsNull() {
        assertNull(MoodRemovalTriggerParser.parseLine("gain_effect Not A Real Effect => hottub"))
    }
}
