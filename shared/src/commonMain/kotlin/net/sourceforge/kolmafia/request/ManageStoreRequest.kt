package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.session.StoreManager

open class ManageStoreRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
) {

    open suspend fun addItem(
        itemId: Int,
        price: Int,
        limit: Int = 0,
        quantity: Int = 1,
        fromStorage: Boolean = false,
    ): Result<String> {
        val result = submitBackoffice(
        parameters = Parameters.build {
            append("action", "additem")
            append("ajax", "1")
            append("itemid", if (fromStorage) "h$itemId" else itemId.toString())
            append("price", price.toString())
            append("quantity", quantity.toString())
            if (limit > 0) append("limit", limit.toString())
        }
        )
        result.getOrNull()?.let { body ->
            if (body.contains("Are you sure you want to sell this item for that little Meat?", true)) {
                return Result.failure(IllegalStateException("KoL's low price protection stopped the sale."))
            }
            parseResponse(
                "backoffice.php?action=additem&itemid=${if (fromStorage) "h" else ""}$itemId&quantity=$quantity",
                body,
            )
        }
        return result
    }

    open suspend fun removeItem(itemId: Int, quantity: Int): Result<String> {
        val amount = minOf(quantity.coerceAtLeast(0), StoreManager.shopAmount(itemId).takeIf { it > 0 } ?: quantity)
        val result = submitBackoffice(
        parameters = Parameters.build {
            append("action", "removeitem")
            append("ajax", "1")
            append("itemid", itemId.toString())
            append("qty", amount.toString())
        }
        )
        result.getOrNull()?.let { parseResponse("backoffice.php?action=removeitem&itemid=$itemId&qty=$amount", it) }
        return result
    }

    open suspend fun repriceItem(itemId: Int, price: Int, limit: Int = 0): Result<String> =
        submitBackoffice(
            parameters = Parameters.build {
                append("action", "updateinv")
                append("ajax", "1")
                append("price[$itemId]", price.toString())
                if (limit > 0) append("limit[$itemId]", limit.toString())
            }
        ).also { result ->
            result.getOrNull()?.let {
                StoreManager.updateSomePrices(it)
                StoreManager.calculatePotentialEarnings()
            }
        }

    open suspend fun refreshPrices(): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/manageprices.php",
            formParameters = Parameters.build { append("action", "refresh") }
        )
        if (response.status.isSuccess()) Result.success(response.bodyAsText().also {
            StoreManager.update(it, StoreManager.TableType.PRICER)
        })
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun getStoreLog(): Result<List<String>> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/backoffice.php",
            formParameters = Parameters.build { append("which", "3") },
        )
        if (!response.status.isSuccess()) Result.failure(Exception("HTTP ${response.status.value}"))
        else {
            StoreManager.parseLog(response.bodyAsText())
            Result.success(StoreManager.getStoreLog().map { it.toString() })
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun parseResponse(url: String, html: String): Boolean {
        val action = Regex("""action=([^&]+)""").find(url)?.groupValues?.get(1)
        if (action == null) {
            StoreManager.update(html, StoreManager.TableType.DEETS)
            return html.isNotBlank()
        }
        if (action == "additem") {
            val stocked = STOCKED.find(html) ?: return false
            val quantity = stocked.groupValues[1].replace(",", "").toInt()
            val price = stocked.groupValues[3].replace(",", "").toLong()
            val limit = stocked.groupValues[5].replace(",", "").toIntOrNull() ?: 0
            val item = ITEM_ID.find(url) ?: return false
            val fromStorage = item.groupValues[1].isNotEmpty()
            val itemId = item.groupValues[2].toInt()
            if (!fromStorage) inventoryManager?.consumeItemLocally(itemId, quantity)
            StoreManager.addItem(itemId, quantity, price, limit)
            return true
        }
        if (action == "removeitem") {
            val itemId = ITEM_ID.find(url)?.groupValues?.get(2)?.toIntOrNull() ?: return false
            val quantity = Regex("""qty=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            inventoryManager?.gainItemLocally(itemId, quantity)
            StoreManager.removeItem(itemId, quantity)
            return true
        }
        if (action == "updateinv") {
            StoreManager.updateSomePrices(html)
            return html.isNotBlank()
        }
        return false
    }

    private suspend fun submitBackoffice(parameters: Parameters): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/backoffice.php",
            formParameters = parameters,
        )
        if (response.status.isSuccess()) Result.success(response.bodyAsText())
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private val ITEM_ID = Regex("""itemid=(h)?(\d+)""", RegexOption.IGNORE_CASE)
        private val STOCKED = Regex(
            """\(([\d,]+)\) (.*?) stocked for ([\d,]+) meat each( \(([\d,]+)/day\))?""",
            RegexOption.IGNORE_CASE,
        )

        fun registerRequest(url: String, itemName: (Int) -> String = { "item #$it" }): String? {
            if (!url.substringAfterLast('/').startsWith("backoffice.php")) return null
            val action = Regex("""action=([^&]+)""").find(url)?.groupValues?.get(1) ?: return null
            val item = ITEM_ID.find(url)
            val itemId = item?.groupValues?.get(2)?.toIntOrNull()
            return when (action) {
                "additem" -> {
                    if (itemId == null) return null
                    val qty = Regex("""quantity=(\d+|\*)""").find(url)?.groupValues?.get(1) ?: "1"
                    val price = Regex("""price=(\d*)""").find(url)?.groupValues?.get(1)?.toLongOrNull() ?: StoreManager.MALL_MAX
                    "Adding $qty ${itemName(itemId)} to store from ${if (item.groupValues[1].isNotEmpty()) "storage" else "inventory"} for $price Meat"
                }
                "removeitem" -> {
                    if (itemId == null) return null
                    val qty = Regex("""qty=(\d+)""").find(url)?.groupValues?.get(1) ?: "1"
                    "Removing $qty ${itemName(itemId)} from store"
                }
                else -> null
            }
        }
    }
}
