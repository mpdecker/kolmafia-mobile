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

    // ── getMallPrice(itemId, maxAgeSeconds) ─────────────────────────────────

    @Test
    fun getMallPrice_maxAge_negativeReturnsPrice() {
        val (mgr, _) = setup()
        mgr.cachePrice(itemId = 1, price = 500L, quantity = 1, shopId = 1)
        assertEquals(500L, mgr.getMallPrice(1, -1L))
    }

    @Test
    fun getMallPrice_maxAge_withinReturnsPrice() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 1, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 100
        assertEquals(500L, mgr.getMallPrice(1, 200L))
    }

    @Test
    fun getMallPrice_maxAge_staleReturnsZero() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 1, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 300
        assertEquals(0L, mgr.getMallPrice(1, 200L))
    }

    @Test
    fun getMallPrice_maxAge_unknownReturnsZero() {
        val (mgr, _) = setup()
        assertEquals(0L, mgr.getMallPrice(999, 200L))
    }

    @Test
    fun getMallPrice_maxAge_exactBoundaryReturnsPrice() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 1, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 200
        assertEquals(500L, mgr.getMallPrice(1, 200L))
    }

    // ── getMallPrice(itemId, maxAgeSeconds, forceUpdate) ────────────────────

    @Test
    fun getMallPrice_forceUpdate_ignoresAge() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 1, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 9999
        // Without force: stale
        assertEquals(0L, mgr.getMallPrice(1, 100L, forceUpdate = false))
        // With force: returns regardless of age (still within TTL)
        assertEquals(500L, mgr.getMallPrice(1, 100L, forceUpdate = true))
    }

    @Test
    fun getMallPrice_forceUpdate_false_behavesLikeAgeGated() {
        val (mgr, clock) = setup()
        mgr.cachePrice(itemId = 1, price = 500L, quantity = 1, shopId = 1)
        clock.nowSeconds += 50
        assertEquals(500L, mgr.getMallPrice(1, 100L, forceUpdate = false))
    }

    // ── cachePriceIfFromCurrentDay ──────────────────────────────────────────

    @Test
    fun cachePriceIfFromCurrentDay_matchingDay_caches() {
        val (mgr, _) = setup()
        mgr.cachePriceIfFromCurrentDay(itemId = 1, price = 300L, quantity = 2, shopId = 5, dayNumber = 7, currentDay = 7)
        val cached = mgr.getCachedPrice(1)
        assertNotNull(cached)
        assertEquals(300L, cached.price)
    }

    @Test
    fun cachePriceIfFromCurrentDay_mismatchDay_skips() {
        val (mgr, _) = setup()
        mgr.cachePriceIfFromCurrentDay(itemId = 1, price = 300L, quantity = 2, shopId = 5, dayNumber = 6, currentDay = 7)
        assertNull(mgr.getCachedPrice(1))
    }
}
