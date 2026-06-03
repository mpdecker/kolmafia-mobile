package net.sourceforge.kolmafia.shop

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CoinmasterRequestTest {

    private fun captureClient(): Pair<HttpClient, MutableList<String>> {
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("<html>success</html>", HttpStatusCode.OK)
        }
        return HttpClient(engine) to bodies
    }

    private fun mockClient(): HttpClient {
        val engine = MockEngine { respond("<html>success</html>", HttpStatusCode.OK) }
        return HttpClient(engine)
    }

    @Test
    fun buy_sendsCorrectFormFields() = runTest {
        val (client, bodies) = captureClient()
        val dmt = CoinmasterRegistry.findByNickname("dmt")!!

        CoinmasterRequest(client).buy(dmt, rowId = 1, quantity = 2)

        val body = bodies[0]
        assertTrue(body.contains("whichshop=dmt"), "body: $body")
        assertTrue(body.contains("action=buyitem"), "body: $body")
        assertTrue(body.contains("whichrow=1"), "body: $body")
        assertTrue(body.contains("quantity=2"), "body: $body")
        assertTrue(body.contains("ajax=1"), "body: $body")
    }

    @Test
    fun sell_sendsCorrectAction() = runTest {
        val (client, bodies) = captureClient()
        val dmt = CoinmasterRegistry.findByNickname("dmt")!!

        CoinmasterRequest(client).sell(dmt, rowId = 1, quantity = 1)

        val body = bodies[0]
        assertTrue(body.contains("action=sellitem"), "body: $body")
        assertFalse(body.contains("action=buyitem"), "sell should not use buyitem: $body")
    }

    @Test
    fun buy_shopIdCoinmaster_returnsSuccess() = runTest {
        val dmt = CoinmasterRegistry.findByNickname("dmt")!!
        val result = CoinmasterRequest(mockClient()).buy(dmt, rowId = 1, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun buy_noShopId_returnsFailure() = runTest {
        val noShop = CoinmasterData("Test", "test", null, null, emptyList(), emptyList())
        val result = CoinmasterRequest(mockClient()).buy(noShop, rowId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun sell_shopIdCoinmaster_returnsSuccess() = runTest {
        val dmt = CoinmasterRegistry.findByNickname("dmt")!!
        val result = CoinmasterRequest(mockClient()).sell(dmt, rowId = 1, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun sell_noShopId_returnsFailure() = runTest {
        val noShop = CoinmasterData("Test", "test", null, null, emptyList(), emptyList())
        val result = CoinmasterRequest(mockClient()).sell(noShop, rowId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }
}
