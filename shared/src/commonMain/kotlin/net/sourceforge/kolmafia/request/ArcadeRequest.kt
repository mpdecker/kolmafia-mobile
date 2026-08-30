package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [ArcadeRequest] — Game Grid arcade place.php hub
 * (Phases 2676–2690).
 */
object ArcadeRequest {
    const val GG_TOKEN = 4621
    const val GG_TICKET = 4622

    private val ACTION = Regex("""[?&]action=([^&]*)""")

    fun getTurnsUsed(urlString: String): Int {
        val action = ACTION.find(urlString)?.groupValues?.get(1) ?: return 0
        return if (
            action.contains("demonstar") ||
            action.contains("meteoid") ||
            action.contains("fighters") ||
            action.contains("fist") ||
            action.contains("spacetrip")
        ) {
            5
        } else {
            0
        }
    }

    fun parseResponse(
        urlString: String,
        responseText: String,
        preferences: Preferences,
        resultProcessor: ResultProcessor? = null,
    ) {
        if (!urlString.contains("whichplace=arcade", ignoreCase = true)) return
        val action = ACTION.find(urlString)?.groupValues?.get(1) ?: return
        when (action) {
            "arcade_skeeball" -> {
                if (!responseText.contains("you can't play Skee-Ball", ignoreCase = true)) {
                    resultProcessor?.processItem(GG_TOKEN, -1)
                }
            }
            "arcade_plumber" -> preferences.setBoolean("_defectiveTokenChecked", true)
        }
    }

    fun registerRequest(
        urlString: String,
        sessionLogger: SessionLogger?,
        preferences: Preferences? = null,
        ascensions: Int = 0,
        tokenCount: Int = 0,
    ): Boolean {
        if (!urlString.contains("whichplace=arcade", ignoreCase = true)) return false
        if (preferences != null &&
            preferences.getInt("lastArcadeAscension", -1) != ascensions
        ) {
            preferences.setInt("lastArcadeAscension", ascensions)
        }
        val action = ACTION.find(urlString)?.groupValues?.get(1) ?: return false
        val message = when {
            action == "arcade_skeeball" -> "Playing Skee-Ball ($tokenCount tokens)"
            action == "arcade_plumber" -> "Checking Jackass Plumber for defective token"
            action.contains("spacetrip") -> "Playing Space Trip"
            action.contains("demonstar") -> "Playing DemonStar"
            action.contains("meteoid") -> "Playing Meteoid"
            action.contains("fighters") -> "Playing Fighters of Fighting"
            action.contains("fist") -> "Playing Dungeon Fist"
            else -> "Arcade: $action"
        }
        sessionLogger?.appendRawLine(message)
        return true
    }

    /**
     * Desktop [ArcadeRequest.checkJackassPlumber] — daily defective-token check via arcade place.
     * Returns the URL to visit, or null if already checked / unavailable.
     */
    fun jackassPlumberUrl(
        preferences: Preferences,
        ascensions: Int,
        hasToken: Boolean,
        hasTicket: Boolean,
    ): String? {
        if (preferences.getBoolean("_defectiveTokenChecked", false)) return null
        var unlocked = preferences.getInt("lastArcadeAscension", -1) == ascensions
        val unlockable = unlocked || hasToken || hasTicket
        if (!unlocked && unlockable) {
            preferences.setInt("lastArcadeAscension", ascensions)
            unlocked = true
        }
        return if (unlocked) {
            "place.php?whichplace=arcade&action=arcade_plumber"
        } else {
            null
        }
    }

    /** Session-log lines for Space Trip / DemonStar / Meteoid choice visits. */
    fun logArcadeChoice(choiceId: Int, sessionLogger: SessionLogger?) {
        val name = when (choiceId) {
            in 460..484 -> "Space Trip"
            471 -> "DemonStar"
            in 488..491 -> "Meteoid"
            else -> return
        }
        sessionLogger?.appendRawLine("Arcade choice: $name ($choiceId)")
    }
}

open class ArcadeHttpRequest(private val client: HttpClient) {
    open suspend fun visitPlumber(
        preferences: Preferences,
        resultProcessor: ResultProcessor? = null,
    ): Result<Unit> = try {
        val response = client.get("$KOL_BASE_URL/place.php?whichplace=arcade&action=arcade_plumber")
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            ArcadeRequest.parseResponse(
                "place.php?whichplace=arcade&action=arcade_plumber",
                response.bodyAsText(),
                preferences,
                resultProcessor,
            )
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
