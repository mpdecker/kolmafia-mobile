package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.chat.PlayerIdRegistry

class ProfileRequestTest {

    @AfterTest
    fun cleanup() {
        PlayerIdRegistry.clearForTest()
    }

    @Test
    fun parse_hardcoreCannotInteract() {
        val profile = ProfileRequest.parse(
            html = "<td><b>(Hardcore)</b></td>",
            playerName = "Hero",
            playerId = "1",
        )
        assertTrue(profile.isHardcore)
        assertFalse(profile.inRonin)
        assertFalse(profile.canInteract)
    }

    @Test
    fun parse_roninCannotInteract() {
        val profile = ProfileRequest.parse(
            html = "<b>(In Ronin)</b>",
            playerName = "Hero",
            playerId = "1",
        )
        assertFalse(profile.isHardcore)
        assertTrue(profile.inRonin)
        assertFalse(profile.canInteract)
    }

    @Test
    fun parse_neitherCanInteract() {
        val profile = ProfileRequest.parse(
            html = "<td>Level 30 Seal Clubber</td>",
            playerName = "Hero",
            playerId = "1",
        )
        assertTrue(profile.canInteract)
    }

    @Test
    fun fromPlayerName_hashPrefixUsesId() {
        PlayerIdRegistry.register("Hero", "99")
        val profile = ProfileRequest.fromPlayerName("#99")
        assertEquals("99", profile.playerId)
        assertEquals("Hero", profile.playerName)
    }

    @Test
    fun retrieve_getsShowplayerWho() = runTest {
        var requested = ""
        val client = HttpClient(MockEngine { request ->
            requested = request.url.toString()
            respond("<td><b>(Hardcore)</b></td>", HttpStatusCode.OK)
        })
        val result = ProfileRequest.retrieve(client, "Hero", "42")
        assertTrue(result.isSuccess)
        assertTrue(requested.contains("showplayer.php"))
        assertTrue(requested.contains("who=42"))
        assertTrue(result.getOrNull()!!.isHardcore)
        assertFalse(result.getOrNull()!!.canInteract)
    }
}
