package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager

open class ClosetRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
    private val character: KoLCharacter? = null,
) {

    open suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/closet.php") {
                parameter("action", "put")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                TransferItemSync.parseClosetTransfer(
                    url = "closet.php?action=put&whichitem=$itemId&qty=$quantity",
                    html = body,
                    itemId = itemId,
                    quantity = quantity,
                    inventory = inventoryManager,
                    character = character,
                )
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/closet.php") {
                parameter("action", "take")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                TransferItemSync.parseClosetTransfer(
                    url = "closet.php?action=take&whichitem=$itemId&qty=$quantity",
                    html = body,
                    itemId = itemId,
                    quantity = quantity,
                    inventory = inventoryManager,
                    character = character,
                )
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Take all items from the closet into inventory. */
    open suspend fun emptyCloset(): Result<Int> {
        return try {
            val contents = fetchContents()
            var moved = 0
            for ((itemId, qty) in contents) {
                if (takeOut(itemId, qty).isSuccess) moved += qty
            }
            Result.success(moved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the closet contents from api.php?what=closet.
     * Returns a map of item ID → quantity. Open so tests can override.
     */
    open suspend fun fetchContents(): Map<Int, Int> {
        return try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "closet")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) return emptyMap()
            val rawMap: Map<String, Int> = response.body()
            rawMap.entries.mapNotNull { (k, v) -> k.toIntOrNull()?.to(v) }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
