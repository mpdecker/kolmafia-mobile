package net.sourceforge.kolmafia.npc

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class NpcBuyRequestTest {

    @Test
    fun buy_sendsCorrectFormFields() = runTest {
        val captured = mutableListOf<String>()
        val engine = MockEngine { request ->
            captured += request.body.toByteArray().decodeToString()
            respond("<html>You acquire 1 pretentious paintbrush.</html>", HttpStatusCode.OK)
        }
        NpcBuyRequest(HttpClient(engine)).buy(storeKey = "guildstore1", itemId = 456, quantity = 2)
        val body = captured[0]
        assertTrue(body.contains("whichstore=guildstore1"), "body: $body")
        assertTrue(body.contains("buying=1"), "body: $body")
        assertTrue(body.contains("whichitem=456"), "body: $body")
        assertTrue(body.contains("howmany=2"), "body: $body")
        assertTrue(body.contains("ajax=1"), "body: $body")
    }

    @Test
    fun buy_success_returnsQuantity() = runTest {
        val engine = MockEngine { respond("<html>You acquire the item.</html>", HttpStatusCode.OK) }
        val result = NpcBuyRequest(HttpClient(engine)).buy("guildstore1", 456, 3)
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow())
    }

    @Test
    fun buy_cantAfford_returnsZero() = runTest {
        val engine = MockEngine {
            respond("<html>You can't afford that item.</html>", HttpStatusCode.OK)
        }
        val result = NpcBuyRequest(HttpClient(engine)).buy("guildstore1", 456, 1)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun buy_storeDoesNotCarryItem_returnsZero() = runTest {
        val engine = MockEngine {
            respond("<html>That store doesn't carry that item.</html>", HttpStatusCode.OK)
        }
        val result = NpcBuyRequest(HttpClient(engine)).buy("nosuchstore", 456, 1)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun buy_networkError_returnsFailure() = runTest {
        val engine = MockEngine { throw Exception("connection refused") }
        val result = NpcBuyRequest(HttpClient(engine)).buy("guildstore1", 456, 1)
        assertTrue(result.isFailure)
    }
}
