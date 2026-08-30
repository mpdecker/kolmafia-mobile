package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class FightHtmlParserTest {
    @Test
    fun preservesParagraphTableAndCommentOrder() {
        val document = FightHtmlParser.parse(
            "<p>First &amp; foremost</p><!-- marker -->" +
                "<table><tr><td>Second</td></tr></table><hr><p>Third",
        )
        assertEquals(5, document.events.size)
        assertEquals("First & foremost", document.paragraphs[0].text)
        assertEquals("Second", document.tables[0].text)
        assertEquals("marker", document.comments[0].text)
        assertTrue(document.events[0].position < document.events[1].position)
    }

    @Test
    fun structuralSyncHandlesPartialFightMessages() {
        val preferences = Preferences(MapSettings())
        val changed = FightStructuralSync.apply(
            FightStructuralSync.Context(
                html = "<p>This Chakra is now 75% clean.",
                location = "Your Bung Chakra",
                preferences = preferences,
            ),
        )
        assertTrue(changed)
        assertEquals("75", preferences.getString("crimbo16BungChakraCleanliness", ""))
    }
}
