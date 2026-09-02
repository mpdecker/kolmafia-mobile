package net.sourceforge.kolmafia.modifiers

import kotlin.test.Test
import kotlin.test.assertEquals

class ModifierValuesBitmapCountTest {

    @Test
    fun bitmapCount_popcountsAssignedBits() {
        val values = ModifierValues(bitmaps = mapOf(BitmapModifier.BRIMSTONE to 0b1011))
        assertEquals(3, values.bitmapCount(BitmapModifier.BRIMSTONE))
    }

    @Test
    fun bitmapCount_clowninessMultipliesPopcount() {
        val values = ModifierValues(bitmaps = mapOf(BitmapModifier.CLOWNINESS to 0b11))
        assertEquals(50, values.bitmapCount(BitmapModifier.CLOWNINESS))
    }
}
