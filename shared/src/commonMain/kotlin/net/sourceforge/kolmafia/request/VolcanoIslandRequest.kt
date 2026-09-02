package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.VolcanoIslandRequest]. */
object VolcanoIslandRequest {
    const val SPAGHETTI_CULT_ROBE_ID = 4175
    const val TRICKSTER_TRIKITIXA_ID = 4321

    private const val NPC = "npc"
    private const val SLIME = "getslime"

    private val ACTION_PATTERN = Regex("""(action|subaction)=([^&]*)""", RegexOption.IGNORE_CASE)

    suspend fun getSlime(client: HttpClient): Result<String> = try {
        val response = client.get("$KOL_BASE_URL/volcanoisland.php") {
            parameter("action", NPC)
            parameter("subaction", SLIME)
        }
        if (!response.status.isSuccess()) {
            Result.failure(IllegalStateException("Volcano slime harvest request failed."))
        } else {
            Result.success(response.bodyAsText())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getAdventuresUsed(url: String): Int {
        val action = actionFromUrl(url) ?: return 0
        return when (action) {
            "tniat" -> 0
            "tuba" -> 1
            else -> 0
        }
    }

    fun npcName(characterClass: CharacterClass?): String? = when (characterClass) {
        CharacterClass.SEAL_CLUBBER -> "a Palm Tree Shelter"
        CharacterClass.TURTLE_TAMER -> "a Guy in the Bushes"
        CharacterClass.DISCO_BANDIT -> "a Girl in a Black Dress"
        CharacterClass.ACCORDION_THIEF -> "the Fishing Village"
        CharacterClass.PASTAMANCER -> "a Protestor"
        CharacterClass.SAUCEROR -> "a Boat"
        else -> null
    }

    fun parseResponse(
        url: String,
        html: String,
        state: CharacterState?,
        preferences: Preferences?,
        equipmentManager: EquipmentManager?,
    ) {
        if (!url.contains("volcanoisland.php", ignoreCase = true)) return
        if (!url.contains("action=tniat", ignoreCase = true) &&
            !url.contains("action=npc", ignoreCase = true)
        ) {
            return
        }

        if (state?.isSauceror == true &&
            html.contains("ladle some slime out of one of the drums")
        ) {
            val prefs = preferences ?: return
            val current = prefs.getInt("_slimeVialsHarvested", 0)
            prefs.setInt("_slimeVialsHarvested", (current + 1).coerceAtMost(10))
        }

        if (state?.isPastamancer == true &&
            html.contains("ripping the robe from your shoulders")
        ) {
            equipmentManager?.discardEquipment(SPAGHETTI_CULT_ROBE_ID)
        }
    }

    fun registerRequest(
        url: String,
        sessionLogger: SessionLogger?,
        state: CharacterState? = null,
        adventureCount: Int = 0,
    ): Boolean {
        if (!url.startsWith("volcanoisland.php", ignoreCase = true)) return false
        if (url.contains("subaction=make", ignoreCase = true)) {
            return false
        }

        val message = visitNpcMessage(url, state, adventureCount) ?: return false
        sessionLogger?.appendRawLine(message)
        return true
    }

    private fun visitNpcMessage(url: String, state: CharacterState?, adventureCount: Int): String? {
        var action: String? = null
        var subaction: String? = null
        ACTION_PATTERN.findAll(url).forEach { match ->
            when (match.groupValues.getOrNull(1)?.lowercase()) {
                "action" -> action = match.groupValues.getOrNull(2)
                "subaction" -> subaction = match.groupValues.getOrNull(2)
            }
        }
        if (action != NPC) return null
        if (subaction == null) {
            val name = npcName(state?.characterClassEnum) ?: return null
            return "Visiting $name on the Secret Tropical Island Volcano Lair"
        }
        if (subaction == SLIME && state?.isSauceror == true) {
            return "[$adventureCount] Volcano Island (Drums of Slime)"
        }
        return null
    }

    private fun actionFromUrl(url: String): String? =
        Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase()
}
