package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryWebHtmlTest {

    private fun libWith(client: HttpClient?): GameRuntimeLibrary =
        GameRuntimeLibrary(httpClient = client)

    @Test
    fun loadHtml_returnsBodyOn200() {
        val lib = libWith(HttpClient(MockEngine {
            respond(
                content = "<html>campground</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }))
        assertEquals(
            "<html>campground</html>",
            outputLib(lib, """print(load_html("campground.php"));"""),
        )
    }

    @Test
    fun loadHtml_prependsBaseUrlForRelativePaths() {
        val captured = mutableListOf<String>()
        val lib = libWith(HttpClient(MockEngine { request ->
            captured += request.url.toString()
            respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
        }))
        outputLib(lib, """load_html("api.php");""")
        assertTrue(captured.any { it.startsWith(KOL_BASE_URL) })
    }

    @Test
    fun loadHtml_encodedTrueUsesUrlAsIs() {
        val captured = mutableListOf<String>()
        val full = "https://example.com/page"
        val lib = libWith(HttpClient(MockEngine { request ->
            captured += request.url.toString()
            respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
        }))
        outputLib(lib, """load_html("$full", true);""")
        assertEquals(full, captured.single())
    }

    @Test
    fun loadHtml_returnsEmptyWhenClientIsNull() {
        assertEquals("", outputLib(libWith(null), """print(load_html("api.php"));"""))
    }

    @Test
    fun formField_returnsNamedInputValue() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals(
            "attack",
            outputLib(lib, """print(form_field("<form><input name='action' value='attack'></form>", "action"));"""),
        )
    }

    @Test
    fun formField_returnsEmptyForMissingName() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals(
            "",
            outputLib(lib, """print(form_field("<form></form>", "missing"));"""),
        )
    }

    @Test
    fun makeUrl_buildsQueryString() {
        val lib = GameRuntimeLibrary.forTesting()
        val src = """
            string[string] p;
            p["whichitem"] = "42";
            p["qty"] = "3";
            print(make_url("api.php", p));
        """
        assertEquals("api.php?whichitem=42&qty=3", outputLib(lib, src))
    }

    @Test
    fun makeUrl_returnsBaseWhenParamsEmpty() {
        val lib = GameRuntimeLibrary.forTesting()
        val src = """
            string[string] p;
            print(make_url("campground.php", p));
        """
        assertEquals("campground.php", outputLib(lib, src))
    }
}
