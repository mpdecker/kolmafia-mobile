package net.sourceforge.kolmafia.ash

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryWebRequestTest {

    private fun makeClient(handler: MockRequestHandler): HttpClient =
        HttpClient(MockEngine(handler))

    private fun libWith(client: HttpClient?): GameRuntimeLibrary =
        GameRuntimeLibrary(httpClient = client)

    // ──────────────────────────────────────────────────────────────
    // 1. visit_url returns response body on 200
    // ──────────────────────────────────────────────────────────────
    @Test
    fun visit_url_returnsBodyOn200() {
        val lib = libWith(makeClient {
            respond(
                content = "<html>hello</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        })
        val result = outputLib(lib, """string s = visit_url("api.php"); print(s);""")
        assertEquals("<html>hello</html>", result)
    }

    // ──────────────────────────────────────────────────────────────
    // 2. visit_url encoded=false prepends KOL_BASE_URL
    // ──────────────────────────────────────────────────────────────
    @Test
    fun visit_url_encodedFalse_prependsBaseUrl() {
        val capturedUrls = mutableListOf<String>()
        val lib = libWith(makeClient { request ->
            capturedUrls += request.url.toString()
            respond(
                content = "ok",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        })
        outputLib(lib, """visit_url("api.php");""")
        assertTrue(
            capturedUrls.any { it.startsWith(KOL_BASE_URL) },
            "Expected URL starting with $KOL_BASE_URL but got: $capturedUrls"
        )
    }

    // ──────────────────────────────────────────────────────────────
    // 3. visit_url encoded=true uses URL as-is
    // ──────────────────────────────────────────────────────────────
    @Test
    fun visit_url_encodedTrue_usesUrlAsIs() {
        val capturedUrls = mutableListOf<String>()
        val fullUrl = "https://custom.example.com/page"
        val lib = libWith(makeClient { request ->
            capturedUrls += request.url.toString()
            respond(
                content = "ok",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        })
        outputLib(lib, """visit_url("$fullUrl", true);""")
        assertTrue(
            capturedUrls.any { it.startsWith(fullUrl) },
            "Expected URL starting with $fullUrl but got: $capturedUrls"
        )
    }

    // ──────────────────────────────────────────────────────────────
    // 4. visit_url returns body on 404
    // ──────────────────────────────────────────────────────────────
    @Test
    fun visit_url_returnsBodyOn404() {
        val lib = libWith(makeClient {
            respond(
                content = "not found",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        })
        val result = outputLib(lib, """string s = visit_url("missing.php"); print(s);""")
        assertEquals("not found", result)
    }

    // ──────────────────────────────────────────────────────────────
    // 5. visit_url returns empty string on network error
    // ──────────────────────────────────────────────────────────────
    @Test
    fun visit_url_returnsEmptyOnNetworkError() {
        val lib = libWith(makeClient { throw Exception("Network error") })
        val result = outputLib(lib, """string s = visit_url("api.php"); print(s);""")
        assertEquals("", result)
    }

    // ──────────────────────────────────────────────────────────────
    // 6. visit_url returns empty string when httpClient is null
    // ──────────────────────────────────────────────────────────────
    @Test
    fun visit_url_returnsEmptyWhenClientIsNull() {
        val lib = libWith(null)
        val result = outputLib(lib, """string s = visit_url("api.php"); print(s);""")
        assertEquals("", result)
    }
}
