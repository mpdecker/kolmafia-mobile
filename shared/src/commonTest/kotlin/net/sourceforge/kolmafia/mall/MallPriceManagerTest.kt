package net.sourceforge.kolmafia.mall

import kotlin.test.*

class MallPriceManagerTest {

    private fun setup(nowSeconds: Long = 1_000_000L): Pair<MallPriceManager, MallPriceManager.TestClock> {
        MallPriceDatabase.resetForTest()
        val clock = MallPriceManager.TestClock(nowSeconds)
        return MallPriceManager(clock) to clock
    }

    private fun listing(itemId: Int, price: Long, shopId: Int = 42) = MallListing(
        shopId = shopId,
        shopName = "test shop",
        itemId = itemId,
        price = price,
        quantity = 5,
        limit = 5,
    )

    @Test
    fun getCachedPrice_miss_returnsNull() {
        val (mgr, _) = setup()
        assertNull(mgr.getCachedPrice(itemId = 101))
    }

    @Test
    fun getCachedPrice_hit_withinTtl() {
        val (mgr, _) = setup()
        mgr.cachePrice(itemId = 102, price = 500L, quantity = 3, shopId = 12345)

        val cached = mgr.getCachedPrice(102)
        assertNotNull(cached)
        assertEquals(500L, cached.price)
        assertEquals(12345, cached.shopId)
        assertEquals(3, cached.quantity)
    }

    @Test
    fun getCachedPrice_expired_returnsNull() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 103, price = 500L, quantity = 3, shopId = 12345)

        clock.nowSeconds += MallPriceManager.TTL_SECONDS + 1

        assertNull(mgr.getCachedPrice(itemId = 103))
    }

    @Test
    fun getCachedPrice_exactlyAtTtl_returnsNull() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 104, price = 500L, quantity = 3, shopId = 12345)

        clock.nowSeconds += MallPriceManager.TTL_SECONDS

        assertNull(mgr.getCachedPrice(itemId = 104))
    }

    @Test
    fun cachePrice_upsert_overwritesPrevious() {
        val (mgr, _) = setup()
        mgr.cachePrice(itemId = 105, price = 500L, quantity = 3, shopId = 12345)
        mgr.cachePrice(itemId = 105, price = 450L, quantity = 10, shopId = 67890)

        val cached = mgr.getCachedPrice(105)
        assertNotNull(cached)
        assertEquals(450L, cached.price)
        assertEquals(10, cached.quantity)
        assertEquals(67890, cached.shopId)
    }

    // ── getMallPrice(itemId, maxAgeSeconds) ─────────────────────────────────

    @Test
    fun getMallPrice_maxAge_negativeReturnsPrice() {
        val (mgr, _) = setup()
        mgr.cachePrice(itemId = 201, price = 500L, quantity = 1, shopId = 1)
        assertEquals(500L, mgr.getMallPrice(201, -1L))
    }

    @Test
    fun getMallPrice_maxAge_withinReturnsPrice() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 202, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 100
        assertEquals(500L, mgr.getMallPrice(202, 200L))
    }

    @Test
    fun getMallPrice_maxAge_staleReturnsZero() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 203, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 300
        assertEquals(0L, mgr.getMallPrice(203, 200L))
    }

    @Test
    fun getMallPrice_maxAge_unknownReturnsZero() {
        val (mgr, _) = setup()
        assertEquals(0L, mgr.getMallPrice(999_001, 200L))
    }

    @Test
    fun getMallPrice_maxAge_exactBoundaryReturnsPrice() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 204, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 200
        assertEquals(500L, mgr.getMallPrice(204, 200L))
    }

    // ── getMallPrice(itemId, maxAgeSeconds, forceUpdate) ────────────────────

    @Test
    fun getMallPrice_forceUpdate_ignoresAge() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 301, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 9999
        assertEquals(0L, mgr.getMallPrice(301, 100L, forceUpdate = false))
        assertEquals(500L, mgr.getMallPrice(301, 100L, forceUpdate = true))
    }

    @Test
    fun getMallPrice_forceUpdate_false_behavesLikeAgeGated() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 302, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 50
        assertEquals(500L, mgr.getMallPrice(302, 100L, forceUpdate = false))
    }

    @Test
    fun getMallPrice_forceUpdate_withMallSearchSync_prefetches() {
        val itemId = 9010
        val (mgr, _) = setup()
        mgr.mallSearchSync = { listOf(listing(itemId, 350L)) }
        assertEquals(350L, mgr.getMallPrice(itemId, maxAgeSeconds = 60, forceUpdate = true))
        assertEquals(350L, mgr.getCachedPrice(itemId)?.price)
    }

    @Test
    fun getMallPrice_forceUpdate_withoutSearch_fallsBackToStaleCache() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 303, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 9999
        assertEquals(500L, mgr.getMallPrice(303, 100L, forceUpdate = true))
    }

    @Test
    fun prefetchMallPrice_usesSuspendMallSearch() = kotlinx.coroutines.test.runTest {
        val itemId = 9020
        val (mgr, _) = setup()
        mgr.mallSearch = { listOf(listing(itemId, 275L, shopId = 77)) }
        assertEquals(275L, mgr.prefetchMallPrice(itemId))
        assertEquals(275L, mgr.getCachedPrice(itemId)?.price)
    }

    // ── cachePriceIfFromCurrentDay ──────────────────────────────────────────

    @Test
    fun cachePriceIfFromCurrentDay_matchingDay_caches() {
        val (mgr, _) = setup()
        mgr.cachePriceIfFromCurrentDay(
            itemId = 401, price = 300L, quantity = 2, shopId = 5,
            dayNumber = 7, currentDay = 7,
        )
        val cached = mgr.getCachedPrice(401)
        assertNotNull(cached)
        assertEquals(300L, cached.price)
    }

    @Test
    fun cachePriceIfFromCurrentDay_mismatchDay_skips() {
        val (mgr, _) = setup()
        mgr.cachePriceIfFromCurrentDay(
            itemId = 402, price = 300L, quantity = 2, shopId = 5,
            dayNumber = 6, currentDay = 7,
        )
        assertNull(mgr.getCachedPrice(402))
    }
}
