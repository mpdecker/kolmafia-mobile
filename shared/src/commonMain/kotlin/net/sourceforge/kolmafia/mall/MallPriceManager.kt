package net.sourceforge.kolmafia.mall

class MallPriceManager(private val clock: Clock = SystemClock) {

    /**
     * Optional DI for live mall search when [getMallPrice] is called with [forceUpdate].
     * Invoked from [prefetchMallPrice]; the synchronous overload cannot await network.
     */
    var mallSearch: (suspend (Int) -> List<MallListing>)? = null

    /** Test hook for synchronous force-update prefetch without coroutines. */
    internal var mallSearchSync: ((Int) -> List<MallListing>)? = null

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

    /**
     * Caches the price only when [dayNumber] matches [currentDay] — prevents stale
     * cross-rollover prices from polluting the session cache. Day numbers are
     * caller-supplied rollover-day counters (e.g. `KoLCharacter.currentDays`).
     */
    fun cachePriceIfFromCurrentDay(
        itemId: Int,
        price: Long,
        quantity: Int,
        shopId: Int,
        dayNumber: Int,
        currentDay: Int,
    ) {
        if (dayNumber == currentDay) cachePrice(itemId, price, quantity, shopId)
    }

    fun getCachedPrice(itemId: Int): CachedPrice? {
        val entry = cache[itemId] ?: return null
        if (clock.nowSeconds - entry.cachedAt >= TTL_SECONDS) return null
        return entry.cached
    }

    fun getHistoricalPrice(itemId: Int): Long =
        MallPriceDatabase.getPrice(itemId).takeIf { it > 0 } ?: getCachedPrice(itemId)?.price ?: 0L

    /**
     * Desktop [MallPriceManager.validMallItem] — tradeable mall goods or validated NPC stock.
     * Unknown [ItemDatabase] ids are allowed (catalog not loaded / ASH stubs); known
     * non-tradeable non-NPC ids short-circuit ASH `mall_price` to 0.
     */
    fun validMallItem(itemId: Int): Boolean {
        if (itemId <= 0) return false
        val item = net.sourceforge.kolmafia.data.ItemDatabase.getById(itemId) ?: return true
        if (item.isTradeable) return true
        return net.sourceforge.kolmafia.data.NpcStoreDatabase.containsItem(itemId, validate = true)
    }

    /** Desktop MallPriceManager.getMallPrice — cached mall listing price after prefetch. */
    fun getMallPrice(itemId: Int): Long {
        if (!validMallItem(itemId)) return 0L
        return getHistoricalPrice(itemId)
    }

    /**
     * Desktop MallPriceManager.getMallPrice(itemId, maxAge) — returns cached/historical
     * price only if the recorded age (via [getHistoricalAge]) is within [maxAgeSeconds].
     * If [maxAgeSeconds] < 0 behaves like [getMallPrice] (no age filter).
     * Returns 0 when the price is stale or unknown.
     */
    fun getMallPrice(itemId: Int, maxAgeSeconds: Long): Long {
        if (!validMallItem(itemId)) return 0L
        if (maxAgeSeconds < 0) return getMallPrice(itemId)
        val age = getHistoricalAge(itemId)
        if (age < 0 || age > maxAgeSeconds) return 0L
        return getHistoricalPrice(itemId)
    }

    /**
     * Desktop [MallPriceManager.getMallPrice(AdventureResult)] — for [count] ≤ [NTH_CHEAPEST_COUNT]
     * returns fifth-cheapest × count; larger counts walk a saved search accumulating limits.
     */
    fun getMallPriceForQuantity(itemId: Int, count: Int): Long {
        if (!validMallItem(itemId)) return 0L
        val qty = count.coerceAtLeast(1)
        if (qty <= NTH_CHEAPEST_COUNT) {
            return getMallPrice(itemId) * qty
        }
        val results = getSavedSearch(itemId, needed = qty) ?: return getMallPrice(itemId) * qty
        var needed = qty
        var found = 0
        var lastPrice = -1L
        var total = 0L
        for (listing in results) {
            lastPrice = listing.price
            var available = listing.limit.coerceAtLeast(0)
            if (found < NTH_CHEAPEST_COUNT) {
                if (found + available < NTH_CHEAPEST_COUNT) {
                    found += available
                    continue
                }
                available -= NTH_CHEAPEST_COUNT - found
                found = NTH_CHEAPEST_COUNT
                total = lastPrice * NTH_CHEAPEST_COUNT
                needed -= NTH_CHEAPEST_COUNT
            }
            if (available <= 0) continue
            val used = minOf(available, needed)
            needed -= used
            found += used
            total += lastPrice * used
            if (needed <= 0) return total
        }
        if (needed > 0 && lastPrice > 0) total += lastPrice * needed
        return total
    }

    /**
     * Desktop MallPriceManager.getMallPrice(itemId, maxAge, forceUpdate) soft overload.
     * When [forceUpdate] is true the age gate is ignored. If [mallSearchSync] is wired,
     * performs a headless search and [updateMallPrice]; otherwise returns the last known
     * cache/DB price even past TTL. Live HTTP prefetch requires [prefetchMallPrice].
     * When [forceUpdate] is false behaves like [getMallPrice] with age gate.
     */
    fun getMallPrice(itemId: Int, maxAgeSeconds: Long, forceUpdate: Boolean): Long {
        if (!validMallItem(itemId)) return 0L
        if (forceUpdate) {
            mallSearchSync?.invoke(itemId)?.let { results ->
                saveMallSearch(itemId, results)
                return updateMallPrice(itemId, results)
            }
            MallPriceDatabase.getPrice(itemId).takeIf { it > 0 }?.let { return it }
            return cache[itemId]?.cached?.price ?: 0L
        }
        return getMallPrice(itemId, maxAgeSeconds)
    }

    /** Live mall search + cache refresh when [mallSearch] DI is available. */
    suspend fun prefetchMallPrice(itemId: Int): Long {
        if (itemId <= 0) return 0L
        mallSearch?.invoke(itemId)?.let { results ->
            saveMallSearch(itemId, results)
            return updateMallPrice(itemId, results)
        }
        return getMallPrice(itemId, maxAgeSeconds = -1, forceUpdate = true)
    }

    /** Seconds since the cached price was recorded; -1 if unknown or expired. */
    fun getHistoricalAge(itemId: Int): Long {
        MallPriceDatabase.getAgeSeconds(itemId, clock.nowSeconds)?.let { return it }
        val entry = cache[itemId] ?: return -1L
        if (clock.nowSeconds - entry.cachedAt >= TTL_SECONDS) return -1L
        return clock.nowSeconds - entry.cachedAt
    }

    /**
     * Desktop MallPriceDatabase.getAge — fractional days since price was recorded.
     * Returns [Double.POSITIVE_INFINITY] when unknown (ASH `historical_age` parity).
     */
    fun getHistoricalAgeDays(itemId: Int): Double {
        MallPriceDatabase.getAgeSeconds(itemId, clock.nowSeconds)?.let {
            return it / 86_400.0
        }
        val entry = cache[itemId] ?: return Double.POSITIVE_INFINITY
        if (clock.nowSeconds - entry.cachedAt >= TTL_SECONDS) return Double.POSITIVE_INFINITY
        return (clock.nowSeconds - entry.cachedAt) / 86_400.0
    }

    /**
     * Desktop MallPriceManager.getMallPrice(itemId, maxAge) where [maxAgeDays] is
     * fractional days. Stale DB/cache prices are flushed; forceUpdate refill via
     * [mallSearchSync] or last-known DB when no live search is wired.
     */
    fun getMallPriceDays(itemId: Int, maxAgeDays: Double): Long {
        if (!validMallItem(itemId)) return 0L
        if (maxAgeDays < 0) return getMallPrice(itemId)
        val ageDays = getHistoricalAgeDays(itemId)
        if (ageDays.isFinite() && ageDays <= maxAgeDays) {
            val price = getHistoricalPrice(itemId)
            if (price > 0) return price
        }
        if (ageDays.isFinite() && ageDays > maxAgeDays) {
            flushCache(itemId)
            // Desktop: drop stale mallprices.txt age gate then live-search / forceUpdate.
            MallPriceDatabase.removePrice(itemId)
        }
        return getMallPrice(itemId, maxAgeSeconds = -1, forceUpdate = true)
    }

    /**
     * Desktop mallprices.txt load seed — only current-rollover-day rows enter the session cache.
     * [currentDay] / [dayOf] are caller-supplied rollover-day counters.
     */
    fun seedFromDatabaseIfCurrentDay(
        currentDay: Int,
        dayOf: (timestampSeconds: Long) -> Int = { ts -> (ts / 86_400L).toInt() },
    ) {
        for (entry in MallPriceDatabase.allPrices()) {
            cachePriceIfFromCurrentDay(
                itemId = entry.itemId,
                price = entry.price,
                quantity = 0,
                shopId = 0,
                dayNumber = dayOf(entry.timestampSeconds),
                currentDay = currentDay,
            )
        }
    }

    internal fun cachedAtForTest(itemId: Int): Long? = cache[itemId]?.cachedAt

    fun filterMallSearch(results: List<MallListing>): List<MallListing> =
        results.filter {
            it.source == MallListingSource.MALL && it.shopId > 0 &&
                it.canPurchase && MallPurchaseRequest.canPurchase(it.shopId)
        }.sortedBy { it.price }

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
