package net.sourceforge.kolmafia.chat

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ChatSenderTest {

    private fun mockClientCapturing(): Pair<HttpClient, MutableList<String>> {
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("""{"msgs":[],"last":"0","delay":3000}""", HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(engine) to bodies
    }

    @Test
    fun send_publicMessage_postsCorrectGraf() = runTest {
        val (client, bodies) = mockClientCapturing()
        val sender = ChatSender(httpClient = client)

        sender.send(channel = "clan", message = "hello world")

        assertEquals(1, bodies.size)
        val body = bodies[0]
        // Graf value should encode /clan hello world, j=1 should be present
        assertTrue(body.contains("j=1"), "body: $body")
        assertTrue(body.contains("clan"), "body: $body")
        assertTrue(body.contains("hello+world") || body.contains("hello%20world") || body.contains("hello world"),
            "message text not found in body: $body")
    }

    @Test
    fun send_privateMessage_usesSlashMsg() = runTest {
        val (client, bodies) = mockClientCapturing()
        val sender = ChatSender(httpClient = client)

        sender.sendPrivate(recipient = "Bob", message = "hey")

        val body = bodies[0]
        assertTrue(body.contains("Bob"), "body: $body")
        assertTrue(body.contains("hey"), "body: $body")
        assertTrue(body.contains("msg"), "body: $body")
    }

    @Test
    fun send_returnsSuccess() = runTest {
        val (client, _) = mockClientCapturing()
        val sender = ChatSender(httpClient = client)

        val result = sender.send(channel = "trade", message = "selling stuff")

        assertTrue(result.isSuccess)
    }

    @Test
    fun send_networkError_returnsFailure() = runTest {
        val engine = MockEngine { throw Exception("network error") }
        val sender = ChatSender(httpClient = HttpClient(engine))

        val result = sender.send(channel = "clan", message = "hi")

        assertTrue(result.isFailure)
    }

    @Test
    fun sendPrivate_returnsSuccess() = runTest {
        val (client, _) = mockClientCapturing()
        val sender = ChatSender(httpClient = client)

        val result = sender.sendPrivate(recipient = "Alice", message = "hello")

        assertTrue(result.isSuccess)
    }
}
