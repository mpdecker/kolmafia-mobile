package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.KoLConstants
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.PeeVPeeSync
import net.sourceforge.kolmafia.session.PvpManager
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.PeeVPeeRequest] fight / smashstone HTTP. */
object PeeVPeeRequest {
    val WIN_MESSAGES = arrayOf(
        "50 CHARACTER LIMIT BREAK!",
        "HERE'S YOUR CHEETO, MOTHER!*\$#ER.",
        "If you want it back, I'll be in my tent.",
        "PWNED LIKE CRAPSTORM.",
    )

    val LOSE_MESSAGES = arrayOf(
        "OMG HAX H4X H5X!!",
        "Please return my pants.",
        "How do you like my Crotch-To-Your-Foot style?",
        "PWNED LIKE CRAPSTORM.",
    )

    suspend fun visitFight(
        client: HttpClient,
        character: KoLCharacter?,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        inventoryManager: InventoryManager? = null,
    ): Result<String> = getAndSync(
        client = client,
        url = "peevpee.php?place=fight",
        character = character,
        preferences = preferences,
        sessionLogger = sessionLogger,
        inventoryManager = inventoryManager,
    )

    suspend fun smashStone(
        client: HttpClient,
        character: KoLCharacter?,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        inventoryManager: InventoryManager? = null,
    ): Result<String> = getAndSync(
        client = client,
        url = "peevpee.php?action=smashstone&confirm=on",
        character = character,
        preferences = preferences,
        sessionLogger = sessionLogger,
        inventoryManager = inventoryManager,
    )

    suspend fun fight(
        client: HttpClient,
        opponent: String,
        stance: Int,
        mission: String,
        tougher: Boolean,
        character: KoLCharacter?,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        ranked: String? = null,
        inventoryManager: InventoryManager? = null,
    ): Result<String> {
        val rankedValue = ranked ?: if (tougher) "2" else "1"
        val win = winMessage(preferences)
        val lose = loseMessage(preferences)
        val relativeUrl = fightUrl(opponent, stance, mission, rankedValue)
        sessionLogger?.appendRawLine(fightStartMessage(opponent, rankedValue, mission, stance))
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/peevpee.php",
                formParameters = parameters {
                    append("action", "fight")
                    append("place", "fight")
                    append("attacktype", mission)
                    append("ranked", rankedValue)
                    append("stance", stance.toString())
                    append("who", opponent)
                    append("winmessage", win)
                    append("losemessage", lose)
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val html = response.bodyAsText()
            PeeVPeeSync.apply(html, relativeUrl, character, preferences, sessionLogger, inventoryManager)
            Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun fightStartMessage(
        opponent: String,
        ranked: String,
        mission: String,
        stance: Int,
    ): String = "Attack ${opponentLabel(opponent, ranked)} for ${missionLabel(mission)} via ${stanceLabel(stance)}"

    internal fun opponentLabel(who: String, ranked: String): String = when {
        who.isNotEmpty() -> who
        ranked == "1" -> "a random opponent"
        ranked == "2" -> "a random stronger opponent"
        else -> "an unknown opponent"
    }

    internal fun missionLabel(mission: String): String =
        if (mission == "lootwhatever") "loot" else mission.ifEmpty { "an unknown mission" }

    internal fun stanceLabel(stance: Int): String =
        PvpManager.findStance(stance) ?: "an unknown stance"

    private fun fightUrl(opponent: String, stance: Int, mission: String, ranked: String): String =
        "peevpee.php?action=fight&place=fight&attacktype=$mission&ranked=$ranked&stance=$stance&who=$opponent"

    private suspend fun getAndSync(
        client: HttpClient,
        url: String,
        character: KoLCharacter?,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
        inventoryManager: InventoryManager? = null,
    ): Result<String> = try {
        val response = client.get("$KOL_BASE_URL/$url")
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val html = response.bodyAsText()
            PeeVPeeSync.apply(html, url, character, preferences, sessionLogger, inventoryManager)
            Result.success(html)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun winMessage(preferences: Preferences?): String {
        val pref = preferences?.getString("defaultFlowerWinMessage", "").orEmpty()
        if (pref.isNotEmpty()) return pref
        return WIN_MESSAGES[KoLConstants.RNG.nextInt(WIN_MESSAGES.size)]
    }

    private fun loseMessage(preferences: Preferences?): String {
        val pref = preferences?.getString("defaultFlowerLossMessage", "").orEmpty()
        if (pref.isNotEmpty()) return pref
        return LOSE_MESSAGES[KoLConstants.RNG.nextInt(LOSE_MESSAGES.size)]
    }
}
