package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CombinedModifiersFileTest {

    @BeforeTest
    fun setUp() {
        ModifierDatabase.resetForTest()
        runBlocking { ModifierDatabase.load() }
    }

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun aprilingBandHelmet_fileRowUsesCombinedTags() {
        val raw = ModifierDatabase.getItem("Apriling band helmet")?.modifiers
        assertNotNull(raw)
        assertTrue(raw.contains("Maximum HP / MP"))
        assertTrue(raw.contains("All Attributes Percent"))
        assertFalse(raw.contains("Maximum HP: +25, Maximum MP: +25"))
        assertFalse(raw.contains("Muscle Percent: +10, Mysticality Percent: +10, Moxie Percent: +10"))
    }

    @Test
    fun firemansHelmet_fileRowUsesCombinedPercentTag() {
        val raw = ModifierDatabase.getItem("fireman's helmet")?.modifiers
        assertNotNull(raw)
        assertTrue(raw.contains("All Attributes Percent"))
        assertFalse(raw.contains("Muscle Percent: +10, Mysticality Percent: +10, Moxie Percent: +10"))
    }

    @Test
    fun anniversaryConcreteFedora_fileRowUsesCombinedAttributesTag() {
        val raw = ModifierDatabase.getItem("anniversary concrete fedora")?.modifiers
        assertNotNull(raw)
        assertTrue(raw.contains("All Attributes:"))
        assertFalse(raw.contains("Muscle: +1, Mysticality: +1, Moxie: +1"))
    }

    @Test
    fun aprilingBandHelmet_parsedMuscleAndHpMatchCombinedAmounts() {
        val v = ModifierParser.parse(
            ModifierDatabase.getItem("Apriling band helmet")!!.modifiers,
        )
        assertEquals(25.0, v.get(DoubleModifier.MAXIMUM_HP_MP))
        assertEquals(25.0, v.get(DoubleModifier.HP))
        assertEquals(10.0, v.get(DoubleModifier.ALL_ATTRIBUTES_PCT))
        assertEquals(10.0, v.get(DoubleModifier.MUS_PCT))
    }

    @Test
    fun firemansHelmet_parsedMusclePercentMatchesCombinedAmount() {
        val v = ModifierParser.parse(
            ModifierDatabase.getItem("fireman's helmet")!!.modifiers,
        )
        assertEquals(10.0, v.get(DoubleModifier.ALL_ATTRIBUTES_PCT))
        assertEquals(10.0, v.get(DoubleModifier.MUS_PCT))
    }

    @Test
    fun anniversaryConcreteFedora_parsedMuscleMatchesCombinedAmount() {
        val v = ModifierParser.parse(
            ModifierDatabase.getItem("anniversary concrete fedora")!!.modifiers,
        )
        assertEquals(1.0, v.get(DoubleModifier.ALL_ATTRIBUTES))
        assertEquals(1.0, v.get(DoubleModifier.MUS))
    }
}
