package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OceanRequestTest {

    @Test
    fun sail_postsLonLatToOceanPhp() = runTest {
        var postedLon: String? = null
        var postedLat: String? = null
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertTrue(request.url.encodedPath.endsWith("/ocean.php"))
            val form = request.body as FormDataContent
            postedLon = form.formData["lon"]
            postedLat = form.formData["lat"]
            respond("<html>land ho</html>", HttpStatusCode.OK)
        })
        val request = OceanRequest(client)

        val result = request.sail(63, 29).getOrThrow()
        assertEquals("<html>land ho</html>", result.first)
        assertEquals("63", postedLon)
        assertEquals("29", postedLat)
    }

    @Test
    fun isOceanPage_trueForOceanUrl() {
        assertTrue(
            OceanRequest.isOceanPage(
                html = "<html></html>",
                url = "https://www.kingdomofloathing.com/ocean.php",
            ),
        )
    }

    @Test
    fun isOceanPage_trueForLonLatInputs() {
        val html = """
            <input type=text class=text size=5 name=lon>
            <input type=text class=text size=5 name=lat>
        """.trimIndent()
        assertTrue(OceanRequest.isOceanPage(html, url = "https://example.com/choice.php"))
    }

    @Test
    fun isOceanPage_falseForUnrelatedPage() {
        assertFalse(
            OceanRequest.isOceanPage(
                html = "<html><form></form></html>",
                url = "https://www.kingdomofloathing.com/adventure.php",
            ),
        )
    }
}
