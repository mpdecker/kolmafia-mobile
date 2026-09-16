package net.sourceforge.kolmafia.utilities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PHPRandomSelectionTest {

    @Test
    fun pick_oneRerollsOverflow() {
        val sel = PHPRandomSelection(1341L)
        val picked = sel.pick(20, 1)
        assertEquals(1, picked.size)
        assertTrue(picked[0] in 0 until 20)
    }

    @Test
    fun pick_multipleUsesRandArray() {
        val sel = PHPRandomSelection(42L)
        val picked = sel.pick(10, 3)
        assertEquals(3, picked.size)
        assertEquals(picked.toSet().size, 3)
        assertTrue(picked.all { it in 0 until 10 })
    }
}
