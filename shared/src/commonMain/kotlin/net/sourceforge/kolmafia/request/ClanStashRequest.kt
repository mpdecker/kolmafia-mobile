package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.data.ItemDatabase

open class ClanStashRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
) {

    /** Contribute [quantity] of item [itemId] to the clan stash. */
    open suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/clan_stash.php") {
                parameter("action", "contribute")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                ClanStashSync.parseTransfer(
                    url = "clan_stash.php?action=contribute&whichitem=$itemId&qty=$quantity",
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

    /** Take [quantity] of item [itemId] from the clan stash into the backpack. */
    open suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/clan_stash.php") {
                parameter("action", "take")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                ClanStashSync.parseTransfer(
                    url = "clan_stash.php?action=take&whichitem=$itemId&qty=$quantity",
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

    /** Parses clan_stash.php for item id → quantity. */
    open suspend fun fetchContents(): Map<Int, Int> {
        return try {
            val html = client.get("$KOL_BASE_URL/clan_stash.php").bodyAsText()
            parseAndStoreContents(html)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun parseContents(html: String): Map<Int, Int> {
        return parseContentsStatic(html)
    }

    fun parseAndStoreContents(html: String): Map<Int, Int> {
        val result = parseContentsStatic(html)
        storeContents(result)
        return result
    }

    companion object {
        private val ITEM_ROW = Regex("""whichitem=(\d+)[^>]*>[^<]*(?:\((\d+)\))?""")

        fun parseContentsStatic(html: String): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        for (m in ITEM_ROW.findAll(html)) {
            val id = m.groupValues[1].toIntOrNull() ?: continue
            val qty = m.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: 1
            result[id] = (result[id] ?: 0) + qty
        }
        return result
        }

        fun storeContents(contents: Map<Int, Int>) {
            ClanStashSync.stashCounts = contents.toMutableMap()
            ClanManager.setStash(contents.map { (id, qty) ->
                ClanManager.StashItem(id, ItemDatabase.getItemName(id).ifEmpty { id.toString() }, qty)
            })
        }
    }
}
