package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

class TerminalRequestTest {

    @Test
    fun extrude_campgroundPath_postsVisitThenChoice1191() = runTest {
        val urls = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            urls += request.url.toString()
            bodies += request.body.toByteArray().decodeToString()
            respond("You acquire an item: <b>browser cookie</b>", HttpStatusCode.OK)
        })
        val request = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)

        val result = request.extrude(
            command = "extrude -f food.ext",
            state = CharacterState(),
            preferences = prefs,
        )

        assertTrue(result.isSuccess)
        assertEquals(2, urls.size)
        assertTrue(urls[0].contains("campground.php"))
        assertTrue(bodies[0].contains("action=terminal"))
        assertTrue(urls[1].contains("choice.php"))
        assertTrue(bodies[1].contains("whichchoice=1191"))
        assertTrue(bodies[1].contains("input=extrude+-f+food.ext") || bodies[1].contains("input=extrude%20-f%20food.ext"))
    }

    @Test
    fun extrude_nuclearAutumnPath_usesFalloutShelter() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("You acquire an item.", HttpStatusCode.OK)
        })
        val request = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )

        val result = request.extrude(
            command = "extrude -f food.ext",
            state = CharacterState(challengePath = "Nuclear Autumn"),
            preferences = Preferences(MapSettings()),
            accessibleCount = { id -> if (id == 9033) 1 else 0 },
        )

        assertTrue(result.isSuccess)
        assertTrue(bodies[0].contains("whichplace=falloutshelter"))
        assertTrue(bodies[0].contains("action=vault_term"))
        assertTrue(bodies[1].contains("whichchoice=1191"))
    }

    @Test
    fun extrude_noTerminal_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )

        val result = request.extrude(
            command = "extrude -f food.ext",
            state = CharacterState(),
            preferences = Preferences(MapSettings()),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun terminalCommandForm_containsExpectedFields() {
        val form = TerminalRequest.terminalCommandForm("extrude -f cram.ext")
        assertEquals("1191", form["whichchoice"])
        assertEquals("1", form["option"])
        assertEquals("extrude -f cram.ext", form["input"])
    }

    @Test
    fun enquiry_fam_postsEnquiryCommand() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val request = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)

        val result = request.enquiry("fam", CharacterState(), prefs)
        assertTrue(result.isSuccess)
        assertTrue(bodies.any { it.contains("input=enquiry+familiar.enq") || it.contains("input=enquiry%20familiar.enq") })
    }

    @Test
    fun cliExtrude_incrementsDailyUses() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)

        val result = request.cliExtrude("booze", CharacterState(), prefs)
        assertTrue(result.isSuccess)
        assertEquals(1, prefs.getInt(TerminalRequest.EXTRUDE_USES_PREF, 0))
    }

    @Test
    fun cliExtrude_atLimit_failsWithoutPost() = runTest {
        var posts = 0
        val client = HttpClient(MockEngine {
            posts++
            respond("ok", HttpStatusCode.OK)
        })
        val request = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)
        prefs.setInt(TerminalRequest.EXTRUDE_USES_PREF, 3)

        val result = request.cliExtrude("booze", CharacterState(), prefs)
        assertTrue(result.isFailure)
        assertEquals(0, posts)
        assertEquals("Source Terminal extrude limit reached", result.exceptionOrNull()?.message)
    }
}
