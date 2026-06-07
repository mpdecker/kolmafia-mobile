package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ChewRequestTest {

    private fun makeClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler))

    @Test
    fun chew_callsCorrectEndpoint() = runTest {
        val capturedEncodedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedEncodedPaths += request.url.encodedPath
            respond(
                content = "<html>You chew the spleen item.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        ChewRequest(client).chew(itemId = 1, quantity = 1)
        assertTrue(
            capturedEncodedPaths.any { it == "/inv_spleen.php" },
            "Expected endpoint /inv_spleen.php but got: $capturedEncodedPaths"
        )
    }

    @Test
    fun chew_sendsCorrectItemId() = runTest {
        val capturedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            respond(
                content = "<html>You chew the spleen item.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        ChewRequest(client).chew(itemId = 7, quantity = 1)
        assertTrue(
            capturedPaths.any { it.contains("whichitem=7") },
            "Expected whichitem=7 in request path but got: $capturedPaths"
        )
    }

    @Test
    fun chew_returnsSuccessOnOkResponse() = runTest {
        val client = makeClient {
            respond(
                content = "<html>You chew the spleen item.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        val result = ChewRequest(client).chew(itemId = 1, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun chew_returnsFailureOnServerError() = runTest {
        val client = makeClient { respond("", HttpStatusCode.InternalServerError) }
        val result = ChewRequest(client).chew(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun chew_returnsFailureOnNetworkError() = runTest {
        val client = makeClient { throw Exception("Network error") }
        val result = ChewRequest(client).chew(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }
}
