package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DrinkBoozeRequestTest {

    private fun makeClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler))

    @Test
    fun drink_callsCorrectEndpoint() = runTest {
        val capturedEncodedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedEncodedPaths += request.url.encodedPath
            respond(
                content = "<html>You drink the booze.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        DrinkBoozeRequest(client).drink(itemId = 1, quantity = 1)
        assertTrue(
            capturedEncodedPaths.any { it == "/inv_booze.php" },
            "Expected endpoint /inv_booze.php but got: $capturedEncodedPaths"
        )
    }

    @Test
    fun drink_sendsCorrectItemId() = runTest {
        val capturedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            respond(
                content = "<html>You drink the booze.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        DrinkBoozeRequest(client).drink(itemId = 99, quantity = 1)
        assertTrue(
            capturedPaths.any { it.contains("whichitem=99") },
            "Expected whichitem=99 in request path but got: $capturedPaths"
        )
    }

    @Test
    fun drink_returnsSuccessOnOkResponse() = runTest {
        val client = makeClient {
            respond(
                content = "<html>You drink the booze.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        val result = DrinkBoozeRequest(client).drink(itemId = 1, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun drink_returnsFailureOnServerError() = runTest {
        val client = makeClient { respond("", HttpStatusCode.InternalServerError) }
        val result = DrinkBoozeRequest(client).drink(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun drink_returnsFailureOnNetworkError() = runTest {
        val client = makeClient { throw Exception("Network error") }
        val result = DrinkBoozeRequest(client).drink(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }
}
