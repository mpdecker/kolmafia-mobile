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
        val shore = CoinmasterRegistry.findByNickname("shore")!!

        CoinmasterRequest(client).buy(shore, rowId = 176, quantity = 2)

        val body = bodies[0]
        assertTrue(body.contains("whichshop=shore"), "body: $body")
        assertTrue(body.contains("action=buyitem"), "body: $body")
        assertTrue(body.contains("whichrow=176"), "body: $body")
        assertTrue(body.contains("quantity=2"), "body: $body")
        assertTrue(body.contains("ajax=1"), "body: $body")
    }

    @Test
    fun sell_sendsCorrectAction() = runTest {
        val (client, bodies) = captureClient()
        val shore = CoinmasterRegistry.findByNickname("shore")!!

        CoinmasterRequest(client).sell(shore, rowId = 176, quantity = 1)

        val body = bodies[0]
        assertTrue(body.contains("action=sellitem"), "body: $body")
        assertFalse(body.contains("action=buyitem"), "sell should not use buyitem: $body")
    }

    @Test
    fun buy_shopIdCoinmaster_returnsSuccess() = runTest {
        val shore = CoinmasterRegistry.findByNickname("shore")!!
        val result = CoinmasterRequest(mockClient()).buy(shore, rowId = 176, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun buy_noShopId_returnsFailure() = runTest {
        val noShop = CoinmasterData(
            masterName = "Test",
            nickname = "test",
            token = null,
            shopId = null,
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        val result = CoinmasterRequest(mockClient()).buy(noShop, rowId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun sell_shopIdCoinmaster_returnsSuccess() = runTest {
        val shore = CoinmasterRegistry.findByNickname("shore")!!
        val result = CoinmasterRequest(mockClient()).sell(shore, rowId = 176, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun sell_noShopId_returnsFailure() = runTest {
        val noShop = CoinmasterData(
            masterName = "Test",
            nickname = "test",
            token = null,
            shopId = null,
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        val result = CoinmasterRequest(mockClient()).sell(noShop, rowId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun buy_itemFieldCoinmaster_usesWhichitem() = runTest {
        val paths = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            bodies += request.body.toByteArray().decodeToString()
            respond("<html>success</html>", HttpStatusCode.OK)
        })
        val bhh = CoinmasterRegistry.findByNickname("bhh")!!

        CoinmasterRequest(client).buy(bhh, rowId = 2417, quantity = 1)

        assertTrue(paths.any { it.contains("bounty.php") }, "paths: $paths")
        val body = bodies.firstOrNull() ?: ""
        assertTrue(body.contains("whichitem=2417"), "body: $body")
        assertTrue(body.contains("action=buy"), "body: $body")
    }
}
