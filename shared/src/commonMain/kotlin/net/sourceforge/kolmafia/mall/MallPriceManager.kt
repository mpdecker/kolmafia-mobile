package net.sourceforge.kolmafia.mall

class MallPriceManager(private val clock: Clock = SystemClock) {

    interface Clock {
        val nowSeconds: Long
    }

    object SystemClock : Clock {
        override val nowSeconds: Long
            get() = currentEpochSeconds()
    }

    class TestClock(override var nowSeconds: Long) : Clock

    companion object {
        const val TTL_SECONDS = 3_600L
    }

    data class CachedPrice(val price: Long, val quantity: Int, val shopId: Int)

    private data class Entry(val cached: CachedPrice, val cachedAt: Long)

    private val cache = mutableMapOf<Int, Entry>()

    fun cachePrice(itemId: Int, price: Long, quantity: Int, shopId: Int) {
        cache[itemId] = Entry(
            cached = CachedPrice(price = price, quantity = quantity, shopId = shopId),
            cachedAt = clock.nowSeconds
        )
    }

    fun getCachedPrice(itemId: Int): CachedPrice? {
        val entry = cache[itemId] ?: return null
        if (clock.nowSeconds - entry.cachedAt >= TTL_SECONDS) return null
        return entry.cached
    }

    fun getHistoricalPrice(itemId: Int): Long = getCachedPrice(itemId)?.price ?: 0L

    /** Seconds since the cached price was recorded; -1 if unknown or expired. */
    fun getHistoricalAge(itemId: Int): Long {
        val entry = cache[itemId] ?: return -1L
        if (clock.nowSeconds - entry.cachedAt >= TTL_SECONDS) return -1L
        return clock.nowSeconds - entry.cachedAt
    }

    internal fun cachedAtForTest(itemId: Int): Long? = cache[itemId]?.cachedAt
}

internal expect fun currentEpochSeconds(): Long
