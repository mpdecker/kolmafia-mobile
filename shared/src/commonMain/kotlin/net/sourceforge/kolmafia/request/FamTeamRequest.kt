package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.character.FamTeamSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.PokeBoost
import net.sourceforge.kolmafia.character.PokefamBoostSync
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.FamTeamRequest] visit + pokepill feed HTTP. */
object FamTeamRequest {

    suspend fun visit(
        client: HttpClient,
        character: KoLCharacter,
        familiarManager: FamiliarManager? = null,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        inventoryManager: InventoryManager? = null,
    ): Result<Pair<String, String>> {
        if (!character.state.value.inPokefam) {
            return Result.failure(IllegalStateException("Not in Pokefam path"))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/famteam.php")
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val html = response.bodyAsText()
            val url = "famteam.php"
            FamTeamSync.registerRequest(url, sessionLogger)
            parseVisit(
                url = url,
                html = html,
                character = character,
                familiarManager = familiarManager,
                preferences = preferences,
                sessionLogger = sessionLogger,
                inventoryManager = inventoryManager,
            )
            Result.success(html to url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun feed(
        client: HttpClient,
        race: String,
        boost: PokeBoost,
        character: KoLCharacter,
        familiarManager: FamiliarManager? = null,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        inventoryManager: InventoryManager? = null,
    ): Result<Pair<String, String>> {
        if (!character.state.value.inPokefam) {
            return Result.failure(IllegalStateException("Not in Pokefam path"))
        }
        val familiarId = FamiliarDefinitionDatabase.getByName(race)?.id
            ?: return Result.failure(IllegalArgumentException("Unknown familiar race: $race"))
        val itemId = PokeBoost.itemIdFor(boost)
            ?: return Result.failure(IllegalArgumentException("No pokepill item for boost: $boost"))
        val url = "famteam.php?action=feed&fam=$familiarId&iid=$itemId"
        FamTeamSync.registerRequest(url, sessionLogger)
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/famteam.php",
                formParameters = parameters {
                    append("action", "feed")
                    append("fam", familiarId.toString())
                    append("iid", itemId.toString())
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val html = response.bodyAsText()
            parseVisit(
                url = url,
                html = html,
                character = character,
                familiarManager = familiarManager,
                preferences = preferences,
                sessionLogger = sessionLogger,
                inventoryManager = inventoryManager,
            )
            Result.success(html to url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseVisit(
        url: String?,
        html: String,
        character: KoLCharacter,
        familiarManager: FamiliarManager? = null,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        inventoryManager: InventoryManager? = null,
    ) {
        if (!character.state.value.inPokefam) return
        if (url?.contains("famteam.php", ignoreCase = true) != true) return
        PokefamBoostSync.syncFromFeed(url, html, preferences, inventoryManager)
        FamTeamSync.apply(character, html, familiarManager, preferences, sessionLogger)
    }
}
