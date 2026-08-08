package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FloundryAvailabilityTest {

    @AfterTest
    fun tearDown() {
        FloundryAvailability.resetForTest()
    }

    @Test
    fun addFromHtml_parsesFishStockAboveThreshold() {
        val html = """
            <br>1,234 carp
            <br>25 cod
            <br>9 trout
        """.trimIndent()

        FloundryAvailability.addFromHtml(html)

        assertTrue(FloundryAvailability.isAvailable("carpe"))
        assertEquals(123, FloundryAvailability.creatableCount("carpe"))
        assertTrue(FloundryAvailability.isAvailable("codpiece"))
        assertEquals(2, FloundryAvailability.creatableCount("codpiece"))
        assertFalse(FloundryAvailability.isAvailable("troutsers"))
        assertEquals(0, FloundryAvailability.creatableCount("troutsers"))
    }

    @Test
    fun reset_clearsParsedStock() {
        FloundryAvailability.addForTest("carpe", 100)
        FloundryAvailability.reset()
        assertFalse(FloundryAvailability.isAvailable("carpe"))
    }
}
