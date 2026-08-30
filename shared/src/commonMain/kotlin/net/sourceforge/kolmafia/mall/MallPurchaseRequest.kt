package net.sourceforge.kolmafia.mall

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager

class MallPurchaseRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
    private val character: KoLCharacter? = null,
    private val priceManager: MallPriceManager? = null,
) {

    suspend fun buy(shopId: Int, itemId: Int, quantity: Int, price: Long): Result<String> {
        if (!canPurchase(shopId) || quantity <= 0) {
            return Result.failure(IllegalStateException("Mall store #$shopId is unavailable."))
        }
        return try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/mallstore.php",
            formParameters = parameters {
                append("whichstore", shopId.toString())
                append("buying", "1")
                append("whichitem", "$itemId.$price")
                append("itemid", itemId.toString())
                append("quantity", quantity.toString())
                append("price", price.toString())
                append("ajax", "1")
            }
        )
        val body = response.bodyAsText()
        val parsed = parseResponse(shopId, itemId, quantity, body, price)
        if (parsed.error != null) Result.failure(IllegalStateException(parsed.error))
        else Result.success(body)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
    }

    fun parseResponse(shopId: Int, itemId: Int, requested: Int, html: String, unitPrice: Long = 0): PurchaseResult {
        when {
            html.contains("That player will not sell to you", true) -> {
                addIgnoringStore(shopId)
                priceManager?.flushCache(-1, shopId)
                return PurchaseResult(error = "This store is ignoring you.")
            }
            html.contains("inventory is frozen", true) -> {
                addDisabledStore(shopId)
                priceManager?.flushCache(-1, shopId)
                return PurchaseResult(error = "This store is disabled.")
            }
            html.contains("can't afford", true) -> return PurchaseResult(error = "Not enough funds.")
            html.contains("store doesn't", true) || html.contains("failed to yield", true) ->
                return PurchaseResult(error = "The listing is no longer available.")
        }
        val acquired = ACQUIRE_COUNT.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
            ?: if (html.contains("You acquire an item", true) || html.contains("You acquire:", true)) 1
            else if (html.contains("success", true)) requested else 0
        val spent = MEAT_PATTERN.find(html)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
            ?: if (acquired > 0) unitPrice * acquired else 0L
        if (acquired > 0) {
            inventoryManager?.gainItemLocally(itemId, acquired)
            character?.let {
                val state = it.state.value
                it.updateMeat((state.meat.toLong() - spent).coerceAtLeast(0).toInt(), state.storageMeat)
            }
            priceManager?.flushCache(itemId, shopId)
        }
        return PurchaseResult(acquired = acquired, meatSpent = spent)
    }

    data class PurchaseResult(
        val acquired: Int = 0,
        val meatSpent: Long = 0,
        val error: String? = null,
    )

    companion object {
        private val disabledStores = mutableSetOf<Int>()
        private val ignoringStores = mutableSetOf<Int>()
        private val forbiddenStores = mutableSetOf<Int>()
        private val ACQUIRE_COUNT = Regex("""You acquire(?: an item:)?\s*<b>([\d,]+)\s""", RegexOption.IGNORE_CASE)
        private val MEAT_PATTERN = Regex("""You spent ([\d,]+) [Mm]eat""", RegexOption.DOT_MATCHES_ALL)

        fun canPurchase(shopId: Int): Boolean =
            shopId !in disabledStores && shopId !in ignoringStores && shopId !in forbiddenStores

        fun addDisabledStore(shopId: Int) { disabledStores += shopId }
        fun addIgnoringStore(shopId: Int) { ignoringStores += shopId }
        fun addForbiddenStore(shopId: Int) { forbiddenStores += shopId }
        fun removeForbiddenStore(shopId: Int) { forbiddenStores -= shopId }
        fun getForbiddenStores(): Set<Int> = forbiddenStores.toSet()
        fun resetStoreFilters() {
            disabledStores.clear()
            ignoringStores.clear()
            forbiddenStores.clear()
        }

        fun getStoreString(itemId: Int, price: Long): String = "$itemId.$price"
        fun itemFromStoreString(value: String): Int = value.substringBefore('.').toIntOrNull() ?: -1
        fun priceFromStoreString(value: String): Long = value.substringAfter('.', "").toLongOrNull() ?: 0L
        fun getStoreId(url: String): Int =
            Regex("""whichstore\d?=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: -1

        fun registerRequest(url: String, itemName: (Int) -> String = { "item #$it" }): String? {
            if (!url.substringAfterLast('/').startsWith("mallstore.php")) return null
            val encoded = Regex("""whichitem=([\d.]+)""").find(url)?.groupValues?.get(1) ?: return null
            val quantity = Regex("""quantity=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return null
            val id = itemFromStoreString(encoded)
            return "buy $quantity ${itemName(id)} for ${priceFromStoreString(encoded)} each from shop #${getStoreId(url)}"
        }
    }
}
