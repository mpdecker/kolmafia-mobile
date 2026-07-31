package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CafeDatabaseTest {

    @AfterTest
    fun tearDown() {
        CafeDatabase.resetForTest()
    }

    @Test
    fun parseFromText_mapsIdToName() {
        val text = """
            1
            -1	Petite Porter
            -2	Scrawny Stout
        """.trimIndent()
        CafeDatabase.applyParseForTest(text, ConsumableType.DRINK)
        assertEquals("Petite Porter", CafeDatabase.getCafeBoozeName(-1))
        assertEquals("Scrawny Stout", CafeDatabase.getCafeBoozeName(-2))
        assertEquals(-1, CafeDatabase.getDrink("Petite Porter")?.id)
        assertTrue(CafeDatabase.cafeBoozeIds().contains(-1))
    }

    @Test
    fun parseFromText_foodIds() {
        val text = """
            1
            -1	Peche a la Frog
        """.trimIndent()
        CafeDatabase.applyParseForTest(text, ConsumableType.FOOD)
        assertEquals("Peche a la Frog", CafeDatabase.getCafeFoodName(-1))
        assertEquals(-1, CafeDatabase.getFood("Peche a la Frog")?.id)
    }
}
