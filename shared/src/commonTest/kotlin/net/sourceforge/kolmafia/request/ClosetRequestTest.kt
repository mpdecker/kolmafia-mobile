package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ClosetRequestTest {

    private fun makeClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler))

    @Test
    fun putIn_callsCorrectEndpoint() = runTest {
        val capturedEncodedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedEncodedPaths += request.url.encodedPath
            respond(
                content = "<html>Item added to closet.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        ClosetRequest(client).putIn(itemId = 1, quantity = 1)
        assertTrue(
            capturedEncodedPaths.any { it == "/closet.php" },
            "Expected endpoint /closet.php but got: $capturedEncodedPaths"
        )
    }

    @Test
    fun putIn_sendsActionPut() = runTest {
        val capturedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            respond(
                content = "<html>Item added to closet.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        ClosetRequest(client).putIn(itemId = 5, quantity = 3)
        assertTrue(
            capturedPaths.any { it.contains("action=put") },
            "Expected action=put in request path but got: $capturedPaths"
        )
        assertTrue(
            capturedPaths.any { it.contains("whichitem=5") },
            "Expected whichitem=5 in request path but got: $capturedPaths"
        )
    }

    @Test
    fun takeOut_callsCorrectEndpoint() = runTest {
        val capturedEncodedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedEncodedPaths += request.url.encodedPath
            respond(
                content = "<html>Item taken from closet.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        ClosetRequest(client).takeOut(itemId = 1, quantity = 1)
        assertTrue(
            capturedEncodedPaths.any { it == "/closet.php" },
            "Expected endpoint /closet.php but got: $capturedEncodedPaths"
        )
    }

    @Test
    fun takeOut_sendsActionTake() = runTest {
        val capturedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            respond(
                content = "<html>Item taken from closet.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        ClosetRequest(client).takeOut(itemId = 10, quantity = 2)
        assertTrue(
            capturedPaths.any { it.contains("action=take") },
            "Expected action=take in request path but got: $capturedPaths"
        )
        assertTrue(
            capturedPaths.any { it.contains("whichitem=10") },
            "Expected whichitem=10 in request path but got: $capturedPaths"
        )
    }

    @Test
    fun putIn_actionDiffersFromTakeOut() = runTest {
        val putPaths = mutableListOf<String>()
        val takePaths = mutableListOf<String>()
        val putClient = makeClient { request ->
            putPaths += request.url.fullPath
            respond(
                content = "<html>OK</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        val takeClient = makeClient { request ->
            takePaths += request.url.fullPath
            respond(
                content = "<html>OK</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        ClosetRequest(putClient).putIn(itemId = 1, quantity = 1)
        ClosetRequest(takeClient).takeOut(itemId = 1, quantity = 1)
        assertTrue(
            putPaths.any { it.contains("action=put") },
            "putIn should use action=put but got: $putPaths"
        )
        assertTrue(
            takePaths.any { it.contains("action=take") },
            "takeOut should use action=take but got: $takePaths"
        )
    }

    @Test
    fun putIn_returnsSuccessOnOkResponse() = runTest {
        val client = makeClient {
            respond(
                content = "<html>Item added to closet.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        val result = ClosetRequest(client).putIn(itemId = 1, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun putIn_returnsFailureOnServerError() = runTest {
        val client = makeClient { respond("", HttpStatusCode.InternalServerError) }
        val result = ClosetRequest(client).putIn(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun takeOut_returnsSuccessOnOkResponse() = runTest {
        val client = makeClient {
            respond(
                content = "<html>Item taken from closet.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        val result = ClosetRequest(client).takeOut(itemId = 1, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun takeOut_returnsFailureOnServerError() = runTest {
        val client = makeClient { respond("", HttpStatusCode.InternalServerError) }
        val result = ClosetRequest(client).takeOut(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun putIn_returnsFailureOnNetworkError() = runTest {
        val client = makeClient { throw Exception("Network error") }
        val result = ClosetRequest(client).putIn(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun takeOut_returnsFailureOnNetworkError() = runTest {
        val client = makeClient { throw Exception("Network error") }
        val result = ClosetRequest(client).takeOut(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }
}
