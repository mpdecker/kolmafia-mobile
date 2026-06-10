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
        val capturedBodies = mutableListOf<String>()
        val capturedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.encodedPath
            capturedBodies += request.body.toByteArray().decodeToString()
            respond(
                content = "<html>You sell the item.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        AutosellRequest(client).autosell(itemId = 2, quantity = 5)
        val body = capturedBodies.firstOrNull() ?: ""
        assertTrue(
            body.contains("whichitem=2"),
            "Expected item ID in POST body but got: $body"
        )
        assertTrue(
            capturedPaths.any { it == "/sellstuff.php" },
            "Expected endpoint /sellstuff.php but got: $capturedPaths"
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
