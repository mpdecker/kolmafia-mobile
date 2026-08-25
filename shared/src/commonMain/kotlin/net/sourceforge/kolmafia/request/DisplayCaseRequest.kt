package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager

open class DisplayCaseRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
) {

    companion object {
        private val ITEM_ROW = Regex("""whichitem=(\d+)[^>]*>[^<]*(?:\((\d+)\))?""")
    }

    /** Move [quantity] of item [itemId] from backpack into the display case. */
    open suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/displaycollection.php") {
                parameter("action", "put")
                parameter("whichitem", itemId)
                parameter("howmany", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                TransferItemSync.parseDisplayTransfer(
                    url = "displaycollection.php?action=put&whichitem=$itemId&howmany=$quantity",
                    html = body,
                    itemId = itemId,
                    quantity = quantity,
                    inventory = inventoryManager,
                )
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Move [quantity] of item [itemId] from the display case into the backpack. */
    open suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/displaycollection.php") {
                parameter("action", "take")
                parameter("whichitem", itemId)
                parameter("howmany", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                TransferItemSync.parseDisplayTransfer(
                    url = "displaycollection.php?action=take&whichitem=$itemId&howmany=$quantity",
                    html = body,
                    itemId = itemId,
                    quantity = quantity,
                    inventory = inventoryManager,
                )
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Parses displaycollection.php for item id → quantity. */
    open suspend fun fetchContents(): Map<Int, Int> {
        return try {
            val html = client.get("$KOL_BASE_URL/displaycollection.php").bodyAsText()
            parseContents(html)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun parseContents(html: String): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        for (m in ITEM_ROW.findAll(html)) {
            val id = m.groupValues[1].toIntOrNull() ?: continue
            val qty = m.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: 1
            result[id] = (result[id] ?: 0) + qty
        }
        return result
    }
}
