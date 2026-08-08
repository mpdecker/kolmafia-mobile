package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeakeasyAvailabilityTest {

    @AfterTest
    fun tearDown() {
        SpeakeasyAvailability.resetForTest()
    }

    @Test
    fun addFromHtml_parsesDrinkRowsIntoLoungeIds() {
        val html = """
            <form>
              <input name="drink" value="1">
              <input name="drink" value="4">
              <input name="drink" value="7">
            </form>
        """.trimIndent()

        SpeakeasyAvailability.addFromHtml(html)

        assertTrue(SpeakeasyAvailability.isAvailable("glass of &quot;milk&quot;"))
        assertTrue(SpeakeasyAvailability.isAvailable("Lucky Lindy"))
        assertTrue(SpeakeasyAvailability.isAvailable("Ish Kabibble"))
        assertFalse(SpeakeasyAvailability.isAvailable("Bee's Knees"))
        assertEquals(setOf(1, 4, 7), SpeakeasyAvailability.loungeIdsForTest())
    }

    @Test
    fun reset_clearsAvailability() {
        SpeakeasyAvailability.addLoungeId(4)
        assertTrue(SpeakeasyAvailability.isAvailable("Lucky Lindy"))

        SpeakeasyAvailability.reset()

        assertFalse(SpeakeasyAvailability.isAvailable("Lucky Lindy"))
        assertTrue(SpeakeasyAvailability.loungeIdsForTest().isEmpty())
    }

    @Test
    fun isAvailableItemId_matchesLoungeAvailability() {
        SpeakeasyAvailability.addLoungeId(4)

        assertTrue(SpeakeasyAvailability.isAvailableItemId(7592))
        assertFalse(SpeakeasyAvailability.isAvailableItemId(7593))
    }
}
