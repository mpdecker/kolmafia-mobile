package net.sourceforge.kolmafia.chat

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase

class ChatProbeTest {

    @AfterTest
    fun tearDown() {
        PlayerIdRegistry.clearForTest()
    }

    @Test
    fun sendInternalCommand_postsWhoisGraf() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                bodies += request.body.toByteArray().decodeToString()
                respond("This player is currently online.", HttpStatusCode.OK)
            },
        )
        val probe = ChatProbe(client)
        val result = probe.sendInternalCommand("/whois onlyfax")

        assertTrue(result.isSuccess)
        assertEquals("This player is currently online.", result.getOrNull())
        assertTrue(bodies.single().contains("graf=%2Fwhois+onlyfax") || bodies.single().contains("graf=/whois onlyfax"))
    }

    @Test
    fun isPlayerOnline_trueWhenCurrentlyOnline() = runTest {
        val client = HttpClient(
            MockEngine {
                respond("This player is currently online in channel trade.", HttpStatusCode.OK)
            },
        )
        assertTrue(ChatProbe(client).isPlayerOnline("onlyfax"))
    }

    @Test
    fun isPlayerOnline_trueWhenCurrentlyAway() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(
                    "This player is currently away from KoL in channel trade and listening to clan.",
                    HttpStatusCode.OK,
                )
            },
        )
        assertTrue(ChatProbe(client).isPlayerOnline("easyfax"))
    }

    @Test
    fun isPlayerOnline_falseWhenNotFound() = runTest {
        val client = HttpClient(
            MockEngine {
                respond("No player named onlyfax was found.", HttpStatusCode.OK)
            },
        )
        assertFalse(ChatProbe(client).isPlayerOnline("onlyfax"))
    }

    @Test
    fun isPlayerOnline_falseOnEmptyName() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        assertFalse(ChatProbe(client).isPlayerOnline("  "))
    }

    @Test
    fun slashCount_parsesYouHaveCount() = runTest {
        ItemDatabase.load()
        val itemId = GameDatabase().also { it.load() }.item("seal tooth")!!.id
        val client = HttpClient(
            MockEngine {
                respond("You have 42 seal teeth in your clan.", HttpStatusCode.OK)
            },
        )
        assertEquals(42, ChatProbe(client).slashCount(itemId))
    }

    @Test
    fun slashCount_returnsZeroWhenNoMatch() = runTest {
        val client = HttpClient(
            MockEngine {
                respond("No such item.", HttpStatusCode.OK)
            },
        )
        assertEquals(0, ChatProbe(client).slashCount(1))
    }

    @Test
    fun slashCount_returnsZeroForUnknownItemId() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        assertEquals(0, ChatProbe(client).slashCount(-1))
    }

    @Test
    fun whoClan_parsesClanWhoHtml() = runTest {
        val html = """
            <a href="showplayer.php?who=1"><font color='black'>Online Player</font></a>
            <a href="showplayer.php?who=2"><font color='gray'>Away Player</font></a>
        """.trimIndent()
        val client = HttpClient(MockEngine { respond(html, HttpStatusCode.OK) })
        val contacts = ChatProbe(client).whoClan()
        assertEquals(true, contacts["Online Player"])
        assertEquals(false, contacts["Away Player"])
    }

    @Test
    fun lookupPlayerId_registersIdFromWhoisHtml() = runTest {
        val html = """<a href="showplayer.php?who=12345">Onlyfax</a>"""
        val client = HttpClient(MockEngine { respond(html, HttpStatusCode.OK) })
        assertEquals("12345", ChatProbe(client).lookupPlayerId("onlyfax"))
    }
}
