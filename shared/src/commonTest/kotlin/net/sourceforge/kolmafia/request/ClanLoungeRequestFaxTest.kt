package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClanLoungeRequestFaxTest {

    @Test
    fun findFaxOption_mapsDesktopAliases() {
        val lounge = ClanLoungeRequest(HttpClient(MockEngine { respond("ok") }))
        assertEquals(ClanLoungeRequest.SEND_FAX, lounge.findFaxOption("send"))
        assertEquals(ClanLoungeRequest.SEND_FAX, lounge.findFaxOption("put"))
        assertEquals(ClanLoungeRequest.RECEIVE_FAX, lounge.findFaxOption("receive"))
        assertEquals(ClanLoungeRequest.RECEIVE_FAX, lounge.findFaxOption("get"))
        assertEquals(0, lounge.findFaxOption("fax"))
    }

    @Test
    fun receiveFax_postsReceiveForm() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                bodies += request.body.toByteArray().decodeToString()
                respond("received", HttpStatusCode.OK)
            },
        )
        val result = ClanLoungeRequest(client).receiveFax()
        assertTrue(result.isSuccess)
        assertTrue(bodies.single().contains("preaction=receivefax"))
        assertTrue(bodies.single().contains("whichfloor=2"))
    }
}
