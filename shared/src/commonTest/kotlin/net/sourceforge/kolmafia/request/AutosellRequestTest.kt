package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class AutosellRequestTest {

    private fun makeClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler))

    @Test
    fun autosell_sendsItemIdAndQuantity() = runTest {
        val capturedPaths = mutableListOf<String>()
        val capturedEncodedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            capturedEncodedPaths += request.url.encodedPath
            respond(
                content = "<html>You sell the item.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        AutosellRequest(client).autosell(itemId = 2, quantity = 5)
        assertTrue(
            capturedPaths.any { it.contains("whichitem=2") },
            "Expected item ID in request but got: $capturedPaths"
        )
        assertTrue(
            capturedEncodedPaths.any { it == "/sellstuff_ugly.php" },
            "Expected endpoint /sellstuff_ugly.php but got: $capturedEncodedPaths"
        )
    }

    @Test
    fun autosell_returnsSuccessOnOkResponse() = runTest {
        val client = makeClient {
            respond(
                content = "<html>Sold!</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        val result = AutosellRequest(client).autosell(itemId = 2, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun autosell_returnsFailureOnServerError() = runTest {
        val client = makeClient { respond("", HttpStatusCode.InternalServerError) }
        val result = AutosellRequest(client).autosell(itemId = 2, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun autosell_returnsFailureOnNetworkError() = runTest {
        val client = makeClient { throw Exception("Network failure") }
        val result = AutosellRequest(client).autosell(itemId = 2, quantity = 1)
        assertTrue(result.isFailure)
    }
}
