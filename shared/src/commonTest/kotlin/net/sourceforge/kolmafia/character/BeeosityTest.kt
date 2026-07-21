package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BeeosityTest {

    @Test
    fun itemBeeosity_countsLowerAndUpperB() {
        assertEquals(0, Beeosity.itemBeeosity("steel ascot"))
        assertEquals(1, Beeosity.itemBeeosity("beefy"))
        assertEquals(4, Beeosity.itemBeeosity("babbling book"))
    }

    @Test
    fun itemBeeosity_nullOrBlankIsZero() {
        assertEquals(0, Beeosity.itemBeeosity(null))
        assertEquals(0, Beeosity.itemBeeosity(""))
    }

    @Test
    fun hasBeeosity() {
        assertFalse(Beeosity.hasBeeosity("steel ascot"))
        assertTrue(Beeosity.hasBeeosity("babbling book"))
    }

    @Test
    fun equipmentBeeosity_sumsWornItems() {
        val total = Beeosity.equipmentBeeosity(
            mapOf(
                EquipmentSlot.HAT to "beefy hat",
                EquipmentSlot.SHIRT to "babbling book",
            ),
        )
        assertEquals(5, total)
    }
}
