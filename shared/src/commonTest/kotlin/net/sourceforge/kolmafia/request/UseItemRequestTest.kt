package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class UseItemRequestTest {

    private fun makeClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler))

    @Test
    fun use_sendsCorrectItemId() = runTest {
        val capturedPaths = mutableListOf<String>()
        val capturedEncodedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            capturedEncodedPaths += request.url.encodedPath
            respond(
                content = "<html>You use the item.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        UseItemRequest(client).use(itemId = 2, quantity = 1)
        assertTrue(
            capturedPaths.any { it.contains("whichitem=2") },
            "Expected whichitem=2 in request path but got: $capturedPaths"
        )
        assertTrue(
            capturedEncodedPaths.any { it == "/inv_use.php" },
            "Expected endpoint /inv_use.php but got: $capturedEncodedPaths"
        )
    }

    @Test
    fun use_returnsSuccessOnOkResponse() = runTest {
        val client = makeClient {
            respond(
                content = "<html>You use the item.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        val result = UseItemRequest(client).use(itemId = 2, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun use_returnsFailureOnServerError() = runTest {
        val client = makeClient { respond("", HttpStatusCode.InternalServerError) }
        val result = UseItemRequest(client).use(itemId = 2, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun use_returnsFailureOnNetworkError() = runTest {
        val client = makeClient { throw Exception("Network error") }
        val result = UseItemRequest(client).use(itemId = 2, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun feedCandy_sendsCorrectParams() = runTest {
        val capturedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            respond(
                content = "<html>The mimic eats candy.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        UseItemRequest(client).feedCandy(itemId = 42, quantity = 3)
        assertTrue(capturedPaths.any { it.contains("familiarbinger.php") })
        assertTrue(capturedPaths.any { it.contains("whichitem=42") })
        assertTrue(capturedPaths.any { it.contains("action=candy") })
        assertTrue(capturedPaths.any { it.contains("qty=3") })
    }

    @Test
    fun robooze_sendsCorrectParams() = runTest {
        val capturedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            respond(
                content = "<html>Robortender drinks.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        UseItemRequest(client).robooze(itemId = 99)
        assertTrue(capturedPaths.any { it.contains("inventory.php") })
        assertTrue(capturedPaths.any { it.contains("action=robooze") })
        assertTrue(capturedPaths.any { it.contains("whichitem=99") })
        assertTrue(capturedPaths.any { it.contains("ajax=1") })
    }

    @Test
    fun binge_failsWhenFamiliarMismatchInResponse() = runTest {
        val client = makeClient {
            respond(
                content = "You're not currently using a Slimeling.",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val result = UseItemRequest(client).binge(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun multiUse_postsCorrectFormFields() = runTest {
        val bodies = mutableListOf<String>()
        val client = makeClient { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond(
                content = "You acquire an item.",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val result = UseItemRequest(client).multiUse(itemId = 42, quantity = 5)
        assertTrue(result.isSuccess)
        val body = bodies.single()
        assertTrue(body.contains("action=useitem"))
        assertTrue(body.contains("whichitem=42"))
        assertTrue(body.contains("quantity=5"))
    }
}
