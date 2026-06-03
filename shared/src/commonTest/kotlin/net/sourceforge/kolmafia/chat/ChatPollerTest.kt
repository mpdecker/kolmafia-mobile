package net.sourceforge.kolmafia.chat

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ChatPollerTest {

    private fun mockClient(responseJson: String): HttpClient {
        val engine = MockEngine { _ ->
            respond(responseJson, HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(engine)
    }

    @Test
    fun pollOnce_fetchesMessagesAndUpdatesLastTime() = runTest {
        val json = """{"msgs":[
            {"type":"public","who":{"id":"1","name":"A"},
             "channel":"clan","msg":"hi","format":"0","time":"1000"}
        ],"last":"1000","delay":3000}"""
        val poller = ChatPoller(httpClient = mockClient(json))
        val received = mutableListOf<ChatMessage>()
        poller.onMessages { received.addAll(it) }

        poller.pollOnce()

        assertEquals(1, received.size)
        assertEquals("A", received[0].sender)
        assertEquals("1000", poller.lastTime)
    }

    @Test
    fun pollOnce_emptyMessages_noCallback() = runTest {
        val json = """{"msgs":[],"last":"500","delay":3000}"""
        val poller = ChatPoller(httpClient = mockClient(json))
        val received = mutableListOf<ChatMessage>()
        poller.onMessages { received.addAll(it) }

        poller.pollOnce()

        assertTrue(received.isEmpty())
        assertEquals("500", poller.lastTime)
    }

    @Test
    fun pollOnce_networkError_doesNotCrash() = runTest {
        val engine = MockEngine { throw Exception("network error") }
        val poller = ChatPoller(httpClient = HttpClient(engine))

        // Should swallow the exception gracefully; lastTime stays "0"
        poller.pollOnce()
        assertEquals("0", poller.lastTime)
    }
}
