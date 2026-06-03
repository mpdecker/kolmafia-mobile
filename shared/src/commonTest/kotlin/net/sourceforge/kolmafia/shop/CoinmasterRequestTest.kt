package net.sourceforge.kolmafia.shop

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CoinmasterRequestTest {

    private fun mockClient(): HttpClient {
        val engine = MockEngine { respond("<html>success</html>", HttpStatusCode.OK) }
        return HttpClient(engine)
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
