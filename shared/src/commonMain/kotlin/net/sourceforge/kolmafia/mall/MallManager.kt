package net.sourceforge.kolmafia.mall

import net.sourceforge.kolmafia.data.GameDatabase

open class MallManager(
    private val searchRequest: MallSearchRequest,
    private val purchaseRequest: MallPurchaseRequest,
    private val gameDatabase: GameDatabase?
) {
    open suspend fun buy(itemId: Int, count: Int, maxPrice: Int = Int.MAX_VALUE): Int {
        val itemName = gameDatabase?.item(itemId)?.name ?: return 0
        val offers = searchRequest.search(itemName, limit = 50)
            .filter { it.price <= maxPrice && it.quantity > 0 }
            .sortedBy { it.price }
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

    open suspend fun cheapestPrice(itemName: String): Long =
        searchRequest.search(itemName, limit = 5).minOfOrNull { it.price } ?: -1L
}
