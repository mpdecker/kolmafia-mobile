package net.sourceforge.kolmafia.session

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventHistoryTest {

    @AfterTest
    fun tearDown() {
        EventHistory.resetForTest()
    }

    @Test
    fun checkForNewEvents_stripsTagsAndAddsText() {
        EventHistory.checkForNewEvents(ORANGE_EVENTS_HTML)
        assertEquals(listOf("You found a thing."), EventHistory.texts())
    }

    @Test
    fun clear_emptiesHistory() {
        EventHistory.checkForNewEvents(ORANGE_EVENTS_HTML)
        EventHistory.clear()
        assertTrue(EventHistory.texts().isEmpty())
    }

    @Test
    fun skipsLoggedInLines() {
        val html = orangeEvents("Player logged in.")
        EventHistory.checkForNewEvents(html)
        assertTrue(EventHistory.texts().isEmpty())
    }

    companion object {
        fun orangeEvents(inner: String): String =
            """<table><tr><td><b>New Events:</b></td></tr>""" +
                """<tr><td style="padding: 5px; border: 1px solid orange;" align=center>""" +
                inner +
                """</td></tr><tr><td height=4></td></tr></table>"""

        val ORANGE_EVENTS_HTML = orangeEvents("You found a thing.")
    }
}
