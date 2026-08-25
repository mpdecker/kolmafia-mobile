package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class ChewRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    suspend fun chew(itemId: Int, quantity: Int = 1): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/inv_spleen.php") {
                parameter("which", 1)
                parameter("whichitem", itemId)
                parameter("ajax", 1)
                if (quantity > 1) parameter("quantity", quantity)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                UseItemConsumptionSync.rememberLastItem(itemId, quantity)
                val ok = UseItemConsumptionSync.parseConsumption(
                    responseText = body,
                    itemId = itemId,
                    count = quantity,
                    preferences = preferences,
                    character = character,
                    inventory = inventoryManager,
                )
                if (!ok) {
                    Result.failure(
                        IllegalStateException(
                            UseItemConsumptionSync.lastUpdate.ifBlank { "Chew aborted." },
                        ),
                    )
                } else {
                    Result.success(body)
                }
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
