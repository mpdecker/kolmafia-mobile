package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager

open class AutosellRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
    private val character: KoLCharacter? = null,
) {
    enum class Mode(val wireName: String) { QUANTITY("quant"), ALL("all"), ALL_BUT_ONE("allbutone") }

    open suspend fun autosell(itemId: Int, quantity: Int): Result<String> {
        return autosellBatch(mapOf(itemId to quantity), Mode.QUANTITY)
    }

    open suspend fun autosellBatch(items: Map<Int, Int>, mode: Mode = Mode.QUANTITY): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/sellstuff.php",
                formParameters = Parameters.build {
                    append("action", "sell")
                    append("ajax", "1")
                    append("type", mode.wireName)
                    append("howmany", items.values.firstOrNull()?.toString() ?: "1")
                    items.forEach { (itemId, quantity) ->
                        append(if (items.size == 1) "whichitem" else "whichitem[]", itemId.toString())
                        append("quantity", quantity.toString())
                    }
                }
            )
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val url = buildString {
                    append("sellstuff.php?action=sell&type=").append(mode.wireName)
                    append("&howmany=").append(items.values.firstOrNull() ?: 1)
                    items.forEach { (itemId, _) ->
                        append(if (items.size == 1) "&whichitem=" else "&whichitem[]=").append(itemId)
                    }
                }
                AutosellSync.parseDetailed(url, body, inventoryManager, character)
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
