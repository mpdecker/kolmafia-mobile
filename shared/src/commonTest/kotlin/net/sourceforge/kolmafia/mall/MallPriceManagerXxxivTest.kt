package net.sourceforge.kolmafia.mall

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Group F — MallPriceManager forceUpdate sync prefetch DI (Behavioral Deepen XXXIV).
 */
class MallPriceManagerXxxivTest {
    @BeforeTest
    fun setUp() {
        MallPriceDatabase.resetForTest()
    }

    @Test
    fun forceUpdate_usesMallSearchSyncPrefetch() {
        val clock = MallPriceManager.TestClock(1_000L)
        val mall = MallPriceManager(clock)
        mall.cachePrice(42, 999L, 1, 1)
        clock.nowSeconds = 10_000L
        assertEquals(0L, mall.getMallPrice(42, maxAgeSeconds = 60))

        mall.mallSearchSync = { itemId ->
            listOf(MallListing(shopId = 7, shopName = "Test", itemId = itemId, price = 123L, quantity = 5))
        }
        assertEquals(123L, mall.getMallPrice(42, maxAgeSeconds = 60, forceUpdate = true))
        assertEquals(123L, mall.getCachedPrice(42)?.price)
    }

    @Test
    fun forceUpdate_fallsBackToHistoricalWithoutSync() {
        val clock = MallPriceManager.TestClock(1_000L)
        val mall = MallPriceManager(clock)
        mall.cachePrice(55, 400L, 2, 1)
        clock.nowSeconds = 10_000L
        assertEquals(400L, mall.getMallPrice(55, maxAgeSeconds = 60, forceUpdate = true))
    }
}
