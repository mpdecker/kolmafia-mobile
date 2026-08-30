package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.campground.MushroomManager
import net.sourceforge.kolmafia.campground.MushroomPlotSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [MushroomRequest] — knoll_mushrooms.php plant/pick/visit (Phases 3381–3395).
 */
open class MushroomRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val inventory: InventoryManager? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    var responseText: String? = null
        private set

    open suspend fun visit(): Result<String> = request("knoll_mushrooms.php")

    open suspend fun pick(square: Int): Result<String> {
        if (square !in 1..16) return Result.failure(IllegalArgumentException("Squares are numbered from 1 to 16."))
        registerRequest("knoll_mushrooms.php?action=click&pos=${square - 1}", sessionLogger)
        return request(
            "knoll_mushrooms.php",
            parameters {
                append("action", "click")
                append("pos", (square - 1).toString())
            },
        )
    }

    open suspend fun plant(square: Int, sporeIndex: Int): Result<String> {
        if (square !in 1..16) return Result.failure(IllegalArgumentException("Squares are numbered from 1 to 16."))
        val data = MushroomManager.getSporeDataByIndex(sporeIndex)
            ?: return Result.failure(IllegalArgumentException("Unknown spore index."))
        registerRequest(
            "knoll_mushrooms.php?action=plant&pos=${square - 1}&whichspore=$sporeIndex",
            sessionLogger,
            MushroomManager.getSporeName(data),
            square,
        )
        return request(
            "knoll_mushrooms.php",
            parameters {
                append("action", "plant")
                append("pos", (square - 1).toString())
                append("whichspore", sporeIndex.toString())
            },
        )
    }

    private suspend fun request(path: String, form: io.ktor.http.Parameters? = null): Result<String> =
        try {
            val response = if (form == null) {
                client.get("$KOL_BASE_URL/$path")
            } else {
                client.submitForm(url = "$KOL_BASE_URL/$path", formParameters = form)
            }
            if (!response.status.isSuccess()) {
                Result.failure(IllegalStateException("HTTP ${response.status.value}"))
            } else {
                val body = response.bodyAsText()
                responseText = body
                parseResponse(path + (form?.entries()?.joinToString("&") { "${it.key}=${it.value.first()}" }.orEmpty()), body)
                Result.success(body)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    fun parseResponse(urlString: String, responseText: String) {
        if (!urlString.contains("knoll_mushrooms.php", ignoreCase = true)) return
        if (urlString.contains("action=plant", ignoreCase = true)) {
            val sporeIndex = Regex("""whichspore=(\d+)""").find(urlString)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val data = MushroomManager.getSporeDataByIndex(sporeIndex)
            if (data != null && responseText.contains("You plant the spore", ignoreCase = true)) {
                ResultProcessor.processMeat(-MushroomManager.getSporePrice(data).toLong(), character)
            }
        } else if (urlString.contains("action=buyplot", ignoreCase = true) &&
            responseText.contains("It's all yours.", ignoreCase = true)
        ) {
            ResultProcessor.processMeat(-5000L, character)
        }
        character?.let { MushroomManager.parsePlot(responseText, preferences, it, urlString) }
    }

    companion object {
        fun registerRequest(
            urlString: String,
            sessionLogger: SessionLogger? = null,
            sporeName: String? = null,
            square: Int = 0,
        ): Boolean {
            if (!urlString.startsWith("knoll_mushrooms.php")) return false
            val message = when {
                urlString.contains("action=click", ignoreCase = true) -> {
                    val sq = square.takeIf { it > 0 }
                        ?: Regex("""pos=(\d+)""").find(urlString)?.groupValues?.get(1)?.toIntOrNull()?.plus(1)
                        ?: return true
                    "pick $sq"
                }
                urlString.contains("action=plant", ignoreCase = true) -> {
                    val sq = square.takeIf { it > 0 }
                        ?: Regex("""pos=(\d+)""").find(urlString)?.groupValues?.get(1)?.toIntOrNull()?.plus(1)
                        ?: return true
                    val name = sporeName ?: "spore"
                    "plant $sq $name"
                }
                urlString.contains("action=buyplot", ignoreCase = true) -> "Buying a mushroom plot"
                else -> return true
            }
            RequestLogger.updateSessionLog("", sessionLogger)
            RequestLogger.updateSessionLog(message, sessionLogger)
            return true
        }
    }
}
