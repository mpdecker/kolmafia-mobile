package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ClanRumpusRequestTest {

    @Test
    fun visit_success_returnsSuccess() = runTest {
        val client = HttpClient(MockEngine { respond("ok") })
        assertTrue(ClanRumpusRequest(client).visit().isSuccess)
    }

    @Test
    fun visit_networkError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { throw Exception("net") })
        assertTrue(ClanRumpusRequest(client).visit().isFailure)
    }

    @Test
    fun visit_serverError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("err", HttpStatusCode.ServiceUnavailable) })
        assertTrue(ClanRumpusRequest(client).visit().isFailure)
    }

    @Test
    fun visit_hitsCorrectUrl() = runTest {
        var capturedUrl = ""
        val client = HttpClient(MockEngine { req ->
            capturedUrl = req.url.toString()
            respond("ok")
        })
        ClanRumpusRequest(client).visit()
        assertTrue(capturedUrl.contains("clan_basement.php"), "url=$capturedUrl")
    }

    @Test
    fun playJukebox_sendsSongAndSetsPref() = runTest {
        var url = ""
        var body = ""
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            body = req.body.toByteArray().decodeToString()
            respond("ok")
        })
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(
            com.russhwolf.settings.MapSettings(),
        )
        ClanRumpusRequest(client).playJukebox(3, prefs)
        assertTrue(url.contains("clan_rumpus.php"), "url=$url")
        assertTrue(body.contains("preaction=jukebox"), "body=$body")
        assertTrue(body.contains("whichsong=3"), "body=$body")
        assertTrue(prefs.getBoolean("_jukebox", false))
    }
}
