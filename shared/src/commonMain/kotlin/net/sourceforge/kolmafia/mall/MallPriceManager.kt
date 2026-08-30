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
        const val MALL_SEARCH_FRESHNESS_SECONDS = 60L
        const val NTH_CHEAPEST_COUNT = 5
        val VALID_CATEGORIES = setOf(
            "allitems", "food", "booze", "othercon", "weapons", "hats", "shirts",
            "container", "pants", "acc", "offhand", "famequip", "combat", "potions",
            "hprestore", "mprestore", "familiars", "mrstore", "unlockers", "new",
        )
    }

    data class CachedPrice(val price: Long, val quantity: Int, val shopId: Int)

    private data class Entry(val cached: CachedPrice, val cachedAt: Long)
    private data class SearchEntry(val results: List<MallListing>, val savedAt: Long)

    private val cache = mutableMapOf<Int, Entry>()
    private val searches = mutableMapOf<Int, SearchEntry>()

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

    fun getHistoricalPrice(itemId: Int): Long =
        MallPriceDatabase.getPrice(itemId).takeIf { it > 0 } ?: getCachedPrice(itemId)?.price ?: 0L

    /** Desktop MallPriceManager.getMallPrice — cached mall listing price after prefetch. */
    fun getMallPrice(itemId: Int): Long = getHistoricalPrice(itemId)

    /** Seconds since the cached price was recorded; -1 if unknown or expired. */
    fun getHistoricalAge(itemId: Int): Long {
        MallPriceDatabase.getAgeSeconds(itemId, clock.nowSeconds)?.let { return it }
        val entry = cache[itemId] ?: return -1L
        if (clock.nowSeconds - entry.cachedAt >= TTL_SECONDS) return -1L
        return clock.nowSeconds - entry.cachedAt
    }

    internal fun cachedAtForTest(itemId: Int): Long? = cache[itemId]?.cachedAt

    fun filterMallSearch(results: List<MallListing>): List<MallListing> =
        results.filter { it.canPurchase && MallPurchaseRequest.canPurchase(it.shopId) }
            .sortedBy { it.price }

    fun nthCheapestPrice(quantity: Int = NTH_CHEAPEST_COUNT, results: List<MallListing>): Long {
        var needed = quantity.coerceAtLeast(1)
        var last = -1L
        for (listing in filterMallSearch(results)) {
            last = listing.price
            needed -= listing.limit.coerceAtLeast(0)
            if (needed <= 0) return last
        }
        return last
    }

    fun updateMallPrice(itemId: Int, results: List<MallListing>, deferred: Boolean = false): Long {
        if (itemId <= 0) return 0
        val price = nthCheapestPrice(NTH_CHEAPEST_COUNT, results)
        val source = filterMallSearch(results).firstOrNull { it.price == price }
        cache[itemId] = Entry(
            CachedPrice(price, source?.quantity ?: 0, source?.shopId ?: 0),
            clock.nowSeconds,
        )
        if (price > 0) MallPriceDatabase.recordPrice(itemId, price, clock.nowSeconds, deferred)
        return price
    }

    fun saveMallSearch(itemId: Int, results: List<MallListing>) {
        searches[itemId] = SearchEntry(results.toList(), clock.nowSeconds)
    }

    fun getSavedSearch(itemId: Int, needed: Int = 0): List<MallListing>? {
        val entry = searches[itemId] ?: return null
        if (clock.nowSeconds - entry.savedAt >= MALL_SEARCH_FRESHNESS_SECONDS) {
            searches.remove(itemId)
            return null
        }
        val filtered = filterMallSearch(entry.results)
        if (needed <= 0 || filtered.sumOf { it.limit } >= needed) return filtered
        return null
    }

    fun flushCache(itemId: Int) {
        searches.remove(itemId)
        cache.remove(itemId)
    }

    fun flushCache(itemId: Int, shopId: Int) {
        if (itemId > 0) {
            val entry = searches[itemId] ?: return
            val filtered = entry.results.filterNot { it.shopId == shopId }
            if (filtered.isEmpty()) flushCache(itemId)
            else {
                searches[itemId] = entry.copy(results = filtered)
                updateMallPrice(itemId, filtered)
            }
            return
        }
        searches.keys.toList().forEach { flushCache(it, shopId) }
    }

    fun resetMallPrices(shopId: Int) {
        searches.filterValues { entry -> entry.results.any { it.shopId == shopId } }
            .forEach { (itemId, entry) -> updateMallPrice(itemId, entry.results) }
    }

    fun reset() {
        cache.clear()
        searches.clear()
    }
}

internal expect fun currentEpochSeconds(): Long

/** Shared wall-clock source for timestamped session records. */
fun currentEpochMillis(): Long = currentEpochSeconds() * 1000L
