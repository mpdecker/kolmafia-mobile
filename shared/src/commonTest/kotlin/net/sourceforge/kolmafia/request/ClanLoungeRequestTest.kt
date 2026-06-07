package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClanLoungeRequestTest {

    @Test fun useKlaw_success_returnsBody() = runTest {
        val client = HttpClient(MockEngine { respond("klaw body") })
        val result = ClanLoungeRequest(client).useKlaw()
        assertTrue(result.isSuccess)
        assertEquals("klaw body", result.getOrNull())
    }

    @Test fun useKlaw_serverError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("err", HttpStatusCode.InternalServerError) })
        assertTrue(ClanLoungeRequest(client).useKlaw().isFailure)
    }

    @Test fun useKlaw_sendsKlawAction() = runTest {
        var url = ""
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            respond("klaw body")
        })
        ClanLoungeRequest(client).useKlaw()
        assertTrue(url.contains("clan_viplounge.php"), "url=$url")
    }

    @Test fun useLookingGlass_sendsCorrectAction() = runTest {
        var url = ""
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            respond("ok")
        })
        ClanLoungeRequest(client).useLookingGlass()
        assertTrue(url.contains("clan_viplounge.php"), "url=$url")
    }

    @Test fun visitFireworks_sendsCorrectAction() = runTest {
        var url = ""
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            respond("ok")
        })
        ClanLoungeRequest(client).visitFireworks()
        assertTrue(url.contains("clan_viplounge.php"), "url=$url")
    }

    @Test fun playPoolGame_sendsCorrectFormParams() = runTest {
        var url = ""
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            respond("ok")
        })
        ClanLoungeRequest(client).playPoolGame()
        assertTrue(url.contains("clan_viplounge.php"), "url=$url")
    }

    @Test fun useKlaw_networkError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { throw Exception("net") })
        assertTrue(ClanLoungeRequest(client).useKlaw().isFailure)
    }
}
