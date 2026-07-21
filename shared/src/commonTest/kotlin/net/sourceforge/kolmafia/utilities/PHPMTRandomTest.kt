package net.sourceforge.kolmafia.utilities

import kotlin.test.Test
import kotlin.test.assertEquals

class PHPMTRandomTest {

    @Test
    fun nextInt_sameSeedProducesSameSequence() {
        val a = PHPMTRandom(1341L)
        val b = PHPMTRandom(1341L)
        repeat(5) {
            assertEquals(a.nextInt(0, 20), b.nextInt(0, 20))
        }
    }

    @Test
    fun nextInt_poolPickForSeed1341() {
        val rng = PHPMTRandom(1341L)
        assertEquals(10, rng.nextInt(0, 20))
    }

    @Test
    fun nextInt_poolPickForSeed1762() {
        val rng = PHPMTRandom(1762L)
        assertEquals(2, rng.nextInt(0, 2))
    }
}
