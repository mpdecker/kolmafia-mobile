package net.sourceforge.kolmafia.modifiers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FamiliarSlotModifiersTest {

    @Test
    fun forHandSlot_stripsSlimeHatesItAndMoxieMpFlags() {
        val raw = ModifierParser.parse("Mysticality: +5, Slime Hates It: +10, Moxie May Control MP")
        val adjusted = FamiliarSlotModifiers.forHandSlot(raw)
        assertEquals(5.0, adjusted.get(DoubleModifier.MYS))
        assertEquals(0.0, adjusted.get(DoubleModifier.SLIME_HATES_IT))
        assertFalse(adjusted.get(BooleanModifier.MOXIE_MAY_CONTROL_MP))
    }

    @Test
    fun forHandSlot_stripsBitmapSynergyFlags() {
        val raw = ModifierParser.parse("Mysticality: +3, Brimstone, Cloathing, Synergetic")
        val adjusted = FamiliarSlotModifiers.forHandSlot(raw)
        assertEquals(3.0, adjusted.get(DoubleModifier.MYS))
        assertEquals(0, adjusted.get(BitmapModifier.BRIMSTONE))
        assertEquals(0, adjusted.get(BitmapModifier.CLOATHING))
        assertEquals(0, adjusted.get(BitmapModifier.SYNERGETIC))
    }
}
