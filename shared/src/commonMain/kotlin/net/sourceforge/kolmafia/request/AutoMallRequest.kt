package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.session.StoreManager

/** Batch inventory-to-store transfer through `managestore.php` (11 rows per request). */
class AutoMallRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
) {
    data class Offer(val itemId: Int, val quantity: Int, val price: Long = StoreManager.MALL_MAX, val limit: Int = 0)

    suspend fun addItems(offers: List<Offer>): Result<Int> {
        var transferred = 0
        for (batch in offers.filter { it.itemId > 0 && it.quantity > 0 }.chunked(CAPACITY)) {
            val parameters = Parameters.build {
                append("action", "additem")
                append("ajax", "1")
                batch.forEachIndexed { index, offer ->
                    append("item$index", offer.itemId.toString())
                    append("qty$index", offer.quantity.toString())
                    append("price$index", if (offer.price == StoreManager.MALL_MAX) "" else offer.price.toString())
                    append("limit$index", if (offer.limit == 0) "" else offer.limit.toString())
                }
            }
            val response = runCatching {
                client.submitForm("$KOL_BASE_URL/managestore.php", parameters).let {
                    if (!it.status.isSuccess()) error("HTTP ${it.status.value}")
                    it.bodyAsText()
                }
            }.getOrElse { return Result.failure(it) }
            if (response.contains("You don't have a store.", true)) {
                return Result.failure(IllegalStateException("You don't have a store."))
            }
            parseTransfer(batch)
            transferred += batch.sumOf { it.quantity }
        }
        return Result.success(transferred)
    }

    private fun parseTransfer(offers: List<Offer>) {
        offers.forEach {
            inventoryManager?.consumeItemLocally(it.itemId, it.quantity)
            StoreManager.addItem(it.itemId, it.quantity, it.price, it.limit)
        }
    }

    companion object {
        const val CAPACITY = 11

        fun registerRequest(url: String): String? =
            if (url.contains("managestore.php", true) && url.contains("action=additem", true)) "mallsell"
            else null
    }
}
