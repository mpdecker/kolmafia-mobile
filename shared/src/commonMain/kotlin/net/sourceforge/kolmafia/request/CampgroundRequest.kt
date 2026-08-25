package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.campground.CampgroundSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

open class CampgroundRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
    private val sessionLogger: SessionLogger? = null,
) {

    /** POSTs campground.php?action=garden. Best-effort: success does not guarantee items existed. */
    open suspend fun harvestGarden(): Result<Unit> =
        visitAction("garden").map { }

    /** POSTs campground.php?action=terminal — opens the Source terminal interface. */
    open suspend fun visitTerminal(): Result<String> = visitAction("terminal")

    /** POSTs campground.php?action=spinningwheel — uses the workshed spinning wheel. */
    open suspend fun useSpinningWheel(): Result<String> = visitAction("spinningwheel")

    /** POSTs campground.php?action=&lt;token&gt; for generic campground CLI actions. */
    open suspend fun visitAction(action: String): Result<String> {
        return try {
            if (RequestAbortGate.abortIfInFightOrChoice()) {
                return Result.failure(
                    IllegalStateException(
                        RequestAbortGate.lastAbortMessage.ifEmpty {
                            "You are currently in a fight or choice."
                        },
                    ),
                )
            }
            val response = client.submitForm(
                url = "$KOL_BASE_URL/campground.php",
                formParameters = parameters {
                    append("action", action)
                },
            )
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                val html = response.bodyAsText()
                val url = "campground.php?action=$action"
                RequestLogger.registerRequest(url, sessionLogger, preferences)
                CampgroundSync.parseResponse(url, html, preferences, character, inventoryManager)
                net.sourceforge.kolmafia.recovery.BetweenBattleInvoker.run(true)
                Result.success(html)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun getAdventuresUsed(url: String, freeRestsRemaining: Int): Int =
            CampgroundSync.getAdventuresUsed(url, freeRestsRemaining)
    }
}
