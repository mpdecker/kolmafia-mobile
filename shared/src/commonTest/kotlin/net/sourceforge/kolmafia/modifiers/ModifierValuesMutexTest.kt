package net.sourceforge.kolmafia.modifiers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModifierValuesMutexTest {
    @Test
    fun plus_overlappingMutexBits_accumulatesViolations() {
        val left = ModifierValues(bitmaps = mapOf(BitmapModifier.MUTEX to 0b0011))
        val right = ModifierValues(bitmaps = mapOf(BitmapModifier.MUTEX to 0b0101))
        val merged = left + right
        assertEquals(0b0111, merged.get(BitmapModifier.MUTEX))
        assertEquals(0b0001, merged.get(BitmapModifier.MUTEX_VIOLATIONS))
    }

    @Test
    fun plus_disjointMutexBits_hasNoViolations() {
        val left = ModifierValues(bitmaps = mapOf(BitmapModifier.MUTEX to 0b0001))
        val right = ModifierValues(bitmaps = mapOf(BitmapModifier.MUTEX to 0b0010))
        val merged = left + right
        assertEquals(0b0011, merged.get(BitmapModifier.MUTEX))
        assertEquals(0, merged.get(BitmapModifier.MUTEX_VIOLATIONS))
    }

    @Test
    fun plus_preservesExistingViolations() {
        val left = ModifierValues(
            bitmaps = mapOf(
                BitmapModifier.MUTEX to 0b0011,
                BitmapModifier.MUTEX_VIOLATIONS to 0b1000,
            ),
        )
        val right = ModifierValues(bitmaps = mapOf(BitmapModifier.MUTEX to 0b0100))
        val merged = left + right
        assertTrue(merged.get(BitmapModifier.MUTEX_VIOLATIONS) and 0b1000 != 0)
    }
}
