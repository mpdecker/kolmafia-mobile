package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class StorageRequestTest {

    private fun makeClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler))

    @Test
    fun withdraw_callsCorrectEndpoint() = runTest {
        val capturedEncodedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedEncodedPaths += request.url.encodedPath
            respond(
                content = "<html>Item withdrawn from storage.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        StorageRequest(client).withdraw(itemId = 1, quantity = 1)
        assertTrue(
            capturedEncodedPaths.any { it == "/storage.php" },
            "Expected endpoint /storage.php but got: $capturedEncodedPaths"
        )
    }

    @Test
    fun withdraw_sendsCorrectItemIdAndAction() = runTest {
        val capturedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            respond(
                content = "<html>Item withdrawn from storage.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        StorageRequest(client).withdraw(itemId = 15, quantity = 4)
        assertTrue(
            capturedPaths.any { it.contains("whichitem=15") },
            "Expected whichitem=15 in request path but got: $capturedPaths"
        )
        assertTrue(
            capturedPaths.any { it.contains("action=take") },
            "Expected action=take in request path but got: $capturedPaths"
        )
    }

    @Test
    fun withdraw_returnsSuccessOnOkResponse() = runTest {
        val client = makeClient {
            respond(
                content = "<html>Item withdrawn from storage.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        val result = StorageRequest(client).withdraw(itemId = 1, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun withdraw_returnsFailureOnServerError() = runTest {
        val client = makeClient { respond("", HttpStatusCode.InternalServerError) }
        val result = StorageRequest(client).withdraw(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun withdraw_returnsFailureOnNetworkError() = runTest {
        val client = makeClient { throw Exception("Network error") }
        val result = StorageRequest(client).withdraw(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }
}
