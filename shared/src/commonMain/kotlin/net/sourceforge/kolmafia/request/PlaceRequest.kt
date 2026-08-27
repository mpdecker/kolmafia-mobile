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
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop PlaceRequest HTTP + parse hub (Phases 2361–2390).
 */
open class PlaceRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    open suspend fun visit(
        whichplace: String,
        action: String? = null,
    ): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(
                IllegalStateException(
                    RequestAbortGate.lastAbortMessage.ifEmpty {
                        "You are currently in a fight or choice."
                    },
                ),
            )
        }
        return try {
            val response = client.get("$KOL_BASE_URL/place.php") {
                parameter("whichplace", whichplace)
                if (!action.isNullOrBlank()) parameter("action", action)
            }
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                val html = response.bodyAsText()
                val url = buildString {
                    append("place.php?whichplace=").append(whichplace)
                    if (!action.isNullOrBlank()) append("&action=").append(action)
                }
                RequestLogger.registerRequest(url, sessionLogger, preferences)
                PlaceSync.parseResponse(url, html, preferences, character, inventoryManager)
                Result.success(html)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun getAdventuresUsed(
            url: String,
            freeRestsRemaining: Int = 0,
            preferences: Preferences? = null,
        ): Int = PlaceSync.getAdventuresUsed(url, freeRestsRemaining, preferences)
    }
}
