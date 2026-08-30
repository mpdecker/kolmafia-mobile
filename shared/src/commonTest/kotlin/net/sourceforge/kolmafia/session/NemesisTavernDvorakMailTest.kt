package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.NemesisRequest
import net.sourceforge.kolmafia.request.TavernRequest

class NemesisTavernDvorakMailTest {
    @Test
    fun nemesisPaperStripsAreLinkedIntoPassword() {
        val prefs = Preferences(MapSettings())
        val values = listOf("a:ONE:b", "b:TWO:c", "c:THREE:d", "d:FOUR:e",
            "e:FIVE:f", "f:SIX:g", "g:SEVEN:h", "h:EIGHT:i")
        NemesisManager.paperStripIds.forEachIndexed { index, id ->
            prefs.setString("lastPaperStrip$id", values[index])
        }
        assertEquals("ONETWOTHREEFOURFIVESIXSEVENEIGHT", NemesisManager.password(prefs))
    }

    @Test
    fun nemesisCaveConsumesSuccessfulDoorOffer() {
        val prefs = Preferences(MapSettings())
        var consumed = 0
        val changed = NemesisRequest.parseResponse(
            "cave.php?action=dodoor1&whichitem=37",
            "The stone slab slides into the ceiling.",
            prefs,
            consumeItem = { _, quantity -> consumed += quantity },
        )
        assertTrue(changed)
        assertEquals(1, consumed)
    }

    @Test
    fun tavernMapsSquareAndTracksGoofballPurchase() {
        assertEquals(
            "The Typical Tavern Cellar (row 2, col 3)",
            TavernRequest.cellarLocationString("cellar.php?action=explore&whichspot=8"),
        )
        val prefs = Preferences(MapSettings())
        TavernRequest.parseResponse(
            "tavern.php?place=susguy",
            "Take some goofballs (for free!)",
            prefs,
            null,
            ascensionNumber = 4,
        )
        assertEquals(0, prefs.getInt("lastGoofballBuy", 0))
        TavernRequest.parseResponse(
            "tavern.php?action=buygoofballs",
            "Here you go, man. If you get caught, you didn't get these from me, man.",
            prefs,
            null,
            ascensionNumber = 4,
        )
        assertEquals(4, prefs.getInt("lastGoofballBuy", 0))
    }

    @Test
    fun dvorakParsesBoardAndFindsNextTile() {
        DvorakManager.reset()
        val labels = "SPAREABCD".map { it }
        val cells = buildString {
            repeat(63) { index ->
                val label = if (index == 0) 'S' else labels[index % labels.size]
                val css = if (index == 0) "cell" else "cell greyed"
                append("<td class='$css'><img alt='Tile labeled \"${label}\"'></td>")
            }
        }
        assertTrue(DvorakManager.parseResponse("tiles.php", cells))
        assertNotNull(DvorakManager.nextStepUrl())
        assertTrue(DvorakManager.nextStepUrl()!!.contains("whichtile=0"))
    }

    @Test
    fun mailboxAndContactsRetainNonUiState() {
        MailManager.clear()
        ContactManager.reset()
        val html = """
            <td valign=top><input name="42"><a href="showplayer.php?who=7"><b>Alice</b></a>
            Date: Monday, January 01, 2024, 01:00AM<br>Hello</td>
            <td valign=top><input name="41"><b>System</b>Date: Sunday</td>
        """.trimIndent()
        assertEquals(2, MailManager.parseMailbox("Inbox", html).size)
        assertEquals("Alice", MailManager.messages().first().sender)
        assertEquals("7", ContactManager.playerId("Alice"))
        val contactHtml = """<b>Contact List</b><a href="showplayer.php?who=8"><b>Bob</b></a>"""
        assertTrue(ContactManager.updateFromHtml(contactHtml).contains("Bob"))
        assertFalse(ContactManager.isMailContact("Alice"))
    }
}
