package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResponsePipelinePhase3710Test {
    @Test
    fun routesHighValuePagesWithoutParsingUnknownUrls() {
        assertEquals("campground", ResponseTextParser.classify("campground.php?action=garden"))
        assertEquals("choice", ResponseTextParser.classify("choice.php?whichchoice=123"))
        assertEquals("description", ResponseTextParser.classify("desc_item.php?whichitem=1"))
        assertEquals("place", ResponseTextParser.classify("place.php?whichplace=chateau"))
        assertEquals(null, ResponseTextParser.classify("unknown.php"))
    }

    @Test
    fun preservesEventHtmlAndTimestampWhileFiltering() {
        EventHistory.resetForTest()
        EventHistory.checkForNewEvents(
            """<table><tr><td><b>New Events:</b></td></tr>
                |<tr><td style="padding: 5px; border: 1px solid orange;"><center><table><tr><td>
                |A <b>new</b> event<br />Logged line
                |</td></tr></table></center></td></tr><tr><td height=4></td></tr></table>"""
                .trimMargin(),
        )
        val entries = EventHistory.entries("new")
        assertEquals(1, entries.size)
        assertEquals("A new event", entries.single().text)
        assertTrue(entries.single().html.contains("<b>new</b>"))
        assertTrue(entries.single().timestampMillis > 0L)
        EventHistory.resetForTest()
    }
}
