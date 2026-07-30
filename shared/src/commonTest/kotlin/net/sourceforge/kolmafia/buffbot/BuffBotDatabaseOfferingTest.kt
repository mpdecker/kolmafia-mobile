package net.sourceforge.kolmafia.buffbot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuffBotDatabaseOfferingTest {

    private fun dbWithOakBot(): BuffBotDatabase {
        val db = BuffBotDatabase.forTest(
            bots = listOf(
                BuffBotEntry("OakBot", "12345", "http://example.com/oak.xml"),
            ),
        )
        db.setOfferingsForTest(
            botName = "OakBot",
            philanthropic = listOf(
                BuffBotOffering(
                    botName = "OakBot",
                    price = 50,
                    philanthropic = true,
                    buffs = listOf("Empathy of the Newt"),
                    turns = listOf(10),
                ),
            ),
            standard = listOf(
                BuffBotOffering(
                    botName = "OakBot",
                    price = 100,
                    philanthropic = false,
                    buffs = listOf("Empathy of the Newt"),
                    turns = listOf(10),
                ),
            ),
        )
        return db
    }

    @Test
    fun getOffering_unknownBot_returnsAmountUnchanged() {
        val result = dbWithOakBot().getOffering("RandomPlayer", 50, emptySet(), null)
        assertEquals(50, result.meatAmount)
        assertNull(result.abortMessage)
        assertNull(result.conversionMessage)
    }

    @Test
    fun getOffering_optedOutBot_returnsAbort() {
        val db = BuffBotDatabase.forTest(
            bots = listOf(
                BuffBotEntry("OptOutBot", "1", BuffBotDatabase.OPTOUT_URL),
            ),
        )
        val result = db.getOffering("OptOutBot", 100, emptySet(), null)
        assertEquals(0, result.meatAmount)
        assertTrue(result.abortMessage?.contains("excluded") == true)
    }

    @Test
    fun getOffering_philanthropicMatch_convertsToStandardPrice() {
        val result = dbWithOakBot().getOffering("OakBot", 50, emptySet(), null)
        assertEquals(100, result.meatAmount)
        assertTrue(result.conversionMessage?.contains("Converted to non-philanthropic request") == true)
    }

    @Test
    fun getOffering_activeEffect_returnsZeroMeat() {
        val active = setOf("empathy of the newt")
        val result = dbWithOakBot().getOffering("OakBot", 50, active, null)
        assertEquals(0, result.meatAmount)
        assertNull(result.conversionMessage)
    }

    @Test
    fun getOffering_nonPhilanthropicPrice_returnsUnchanged() {
        val result = dbWithOakBot().getOffering("OakBot", 100, emptySet(), null)
        assertEquals(100, result.meatAmount)
        assertNull(result.conversionMessage)
    }
}
