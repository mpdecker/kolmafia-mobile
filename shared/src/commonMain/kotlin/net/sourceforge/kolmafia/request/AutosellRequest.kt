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
    open suspend fun autosell(itemId: Int, quantity: Int): Result<String> {
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
                    append("type", "quant")
                    append("howmany", quantity.toString())
                    append("whichitem", itemId.toString())
                    append("quantity", quantity.toString())
                }
            )
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val url = "sellstuff.php?action=sell&whichitem=$itemId&howmany=$quantity&quantity=$quantity"
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
