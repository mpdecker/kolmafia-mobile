package net.sourceforge.kolmafia.mall

import net.sourceforge.kolmafia.data.GameDatabase

open class MallManager(
    private val searchRequest: MallSearchRequest,
    private val purchaseRequest: MallPurchaseRequest,
    private val gameDatabase: GameDatabase?,
    private val priceManager: MallPriceManager? = null,
) {
    open suspend fun buy(itemId: Int, count: Int, maxPrice: Int = Int.MAX_VALUE): Int {
        if (count <= 0 || maxPrice < 0) return 0
        val itemName = gameDatabase?.item(itemId)?.name ?: return 0
        val offers = (priceManager?.getSavedSearch(itemId, count)
            ?: searchRequest.search("\"$itemName\"", limit = 0).also {
                priceManager?.saveMallSearch(itemId, it)
            })
            .filter {
                it.price <= maxPrice && it.quantity > 0 && it.canPurchase &&
                    MallPurchaseRequest.canPurchase(it.shopId)
            }
            .sortedBy { it.price }
        cachePrice(itemId, offers)
        var remaining = count
        for (offer in offers) {
            if (remaining <= 0) break
            val qty = minOf(remaining, offer.quantity)
            val result = purchaseRequest.buy(
                shopId = offer.shopId,
                itemId = itemId,
                quantity = qty,
                price = offer.price
            )
            if (result.isSuccess) remaining -= qty
        }
        return count - remaining
    }

    open suspend fun cheapestPrice(itemName: String): Long {
        val itemId = gameDatabase?.item(itemName)?.id
        val offers = searchRequest.search("\"$itemName\"", limit = 0)
        if (itemId != null) cachePrice(itemId, offers)
        return offers.minOfOrNull { it.price } ?: -1L
    }

    /** Desktop [MallPriceManager.searchMall] listing rows for CLI `searchmall`. */
    open suspend fun searchListings(itemName: String, limit: Int): List<MallListing> {
        val max = if (limit <= 0) 40 else limit
        val offers = searchRequest.search(itemName, max)
        val itemId = gameDatabase?.item(itemName)?.id
        if (itemId != null) cachePrice(itemId, offers)
        return offers
    }

    open suspend fun getMallPrice(itemId: Int, maxAgeDays: Double? = null): Long {
        if (maxAgeDays != null) {
            val historical = priceManager?.getHistoricalPrice(itemId) ?: 0
            val age = priceManager?.getHistoricalAge(itemId) ?: -1
            if (historical > 0 && age >= 0 && age / 86_400.0 <= maxAgeDays) return historical
            priceManager?.flushCache(itemId)
        } else {
            val cached = priceManager?.getCachedPrice(itemId)?.price
            if (cached != null) return cached
        }
        val name = gameDatabase?.item(itemId)?.name ?: return 0
        val fetched = cheapestPrice(name)
        return priceManager?.getMallPrice(itemId)?.takeIf { it != 0L } ?: fetched
    }

    open suspend fun mallPrices(category: String, tiers: String = ""): Map<Int, Long> {
        if (category !in MallPriceManager.VALID_CATEGORIES) return emptyMap()
        val groups = searchRequest.searchCategory(category, tiers).groupBy { it.itemId }
        return groups.mapValues { (itemId, rows) ->
            priceManager?.saveMallSearch(itemId, rows)
            priceManager?.updateMallPrice(itemId, rows, deferred = true)
                ?: rows.minOfOrNull { it.price } ?: -1L
        }.also { MallPriceDatabase.save() }
    }

    /** Desktop MallPriceManager.getMallPrices(item set, max age) return value. */
    open suspend fun refreshMallPrices(itemIds: Collection<Int>, maxAgeDays: Double = 0.0): Int {
        var refreshed = 0
        for (itemId in itemIds.distinct()) {
            if (itemId <= 0) continue
            val cachedAge = priceManager?.getHistoricalAge(itemId) ?: -1
            val cached = priceManager?.getCachedPrice(itemId)?.price ?: 0L
            if (cached > 0L && (maxAgeDays <= 0.0 || cachedAge in 0..(maxAgeDays * 86_400).toInt())) {
                continue
            }
            val name = gameDatabase?.item(itemId)?.name ?: continue
            val offers = searchRequest.search("\"$name\"", limit = 0)
            if (offers.isNotEmpty()) {
                cachePrice(itemId, offers)
                refreshed++
            }
        }
        if (refreshed > 0) MallPriceDatabase.save()
        return refreshed
    }

    private fun cachePrice(itemId: Int, offers: List<MallListing>) {
        if (offers.isEmpty()) return
        priceManager?.saveMallSearch(itemId, offers)
        if (priceManager != null) {
            priceManager.updateMallPrice(itemId, offers)
        } else {
            val best = offers.minByOrNull { it.price } ?: return
            priceManager?.cachePrice(itemId, best.price, best.quantity, best.shopId)
        }
    }
}
