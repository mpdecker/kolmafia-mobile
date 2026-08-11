package net.sourceforge.kolmafia.modifiers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModifierValuesFormatterTest {

    @Test
    fun format_roundTripsDoublesBooleansAndBitmaps() {
        val original = ModifierValues(
            doubles = mapOf(
                DoubleModifier.MEATDROP to 15.0,
                DoubleModifier.ITEMDROP to -5.5,
            ),
            booleans = mapOf(BooleanModifier.SINGLE to true),
            bitmaps = mapOf(BitmapModifier.SYNERGETIC to 3),
        )
        val formatted = ModifierValuesFormatter.format(original)
        val parsed = ModifierParser.parse(formatted)
        assertEquals(15.0, parsed.get(DoubleModifier.MEATDROP))
        assertEquals(-5.5, parsed.get(DoubleModifier.ITEMDROP))
        assertTrue(parsed.get(BooleanModifier.SINGLE))
        assertEquals(3, parsed.get(BitmapModifier.SYNERGETIC))
    }

    @Test
    fun format_skipsMutexBitmaps() {
        val original = ModifierValues(
            doubles = mapOf(DoubleModifier.MEATDROP to 10.0),
            bitmaps = mapOf(
                BitmapModifier.MUTEX to 0b0011,
                BitmapModifier.MUTEX_VIOLATIONS to 0b0001,
            ),
        )
        val formatted = ModifierValuesFormatter.format(original)
        assertFalse(formatted.contains("Mutually Exclusive", ignoreCase = true))
        assertFalse(formatted.contains("Mutex Violations", ignoreCase = true))
        assertTrue(formatted.contains("Meat Drop"))
    }

    @Test
    fun format_emptyValuesReturnsEmptyString() {
        assertEquals("", ModifierValuesFormatter.format(ModifierValues.EMPTY))
    }
}
