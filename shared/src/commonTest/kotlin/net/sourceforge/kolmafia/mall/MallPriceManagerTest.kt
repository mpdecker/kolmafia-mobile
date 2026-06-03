package net.sourceforge.kolmafia.mall

import kotlin.test.*

class MallPriceManagerTest {

    private fun setup(nowSeconds: Long = 1_000_000L): Pair<MallPriceManager, MallPriceManager.TestClock> {
        val clock = MallPriceManager.TestClock(nowSeconds)
        return MallPriceManager(clock) to clock
    }

    @Test
    fun getCachedPrice_miss_returnsNull() {
        val (mgr, _) = setup()
        assertNull(mgr.getCachedPrice(itemId = 1))
    }

    @Test
    fun getCachedPrice_hit_withinTtl() {
        val (mgr, _) = setup()
        mgr.cachePrice(itemId = 1, price = 500L, quantity = 3, shopId = 12345)

        val cached = mgr.getCachedPrice(itemId = 1)
        assertNotNull(cached)
        assertEquals(500L, cached.price)
        assertEquals(12345, cached.shopId)
        assertEquals(3, cached.quantity)
    }

    @Test
    fun getCachedPrice_expired_returnsNull() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 1, price = 500L, quantity = 3, shopId = 12345)

        clock.nowSeconds += MallPriceManager.TTL_SECONDS + 1

        assertNull(mgr.getCachedPrice(itemId = 1))
    }

    @Test
    fun getCachedPrice_exactlyAtTtl_returnsNull() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 1, price = 500L, quantity = 3, shopId = 12345)

        clock.nowSeconds += MallPriceManager.TTL_SECONDS

        assertNull(mgr.getCachedPrice(itemId = 1))
    }

    @Test
    fun cachePrice_upsert_overwritesPrevious() {
        val (mgr, _) = setup()
        mgr.cachePrice(itemId = 1, price = 500L, quantity = 3, shopId = 12345)
        mgr.cachePrice(itemId = 1, price = 450L, quantity = 10, shopId = 67890)

        val cached = mgr.getCachedPrice(itemId = 1)
        assertNotNull(cached)
        assertEquals(450L, cached.price)
        assertEquals(10, cached.quantity)
        assertEquals(67890, cached.shopId)
    }
}
