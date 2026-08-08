package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionMethodGates
import net.sourceforge.kolmafia.data.FloundryAvailability
import net.sourceforge.kolmafia.data.FloundryDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

open class FloundryRequest(private val client: HttpClient) {

    suspend fun purchase(
        itemId: Int,
        state: CharacterState? = null,
        prefs: Preferences? = null,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<String> {
        val name = FloundryDatabase.nameForItemId(itemId)
            ?: return Result.failure(IllegalArgumentException("Unknown Floundry item id: $itemId"))
        preflight(name, itemId, state, prefs, accessibleCount).onFailure { return Result.failure(it) }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/clan_viplounge.php",
                formParameters = parameters {
                    append("preaction", "buyfloundryitem")
                    append("whichitem", itemId.toString())
                },
            )
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                val body = response.bodyAsText()
                if (body.contains("You acquire")) {
                    prefs?.setBoolean(FLOUNDRY_ITEM_CREATED_PREF, true)
                    ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
                }
                Result.success(body)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun purchase(
        name: String,
        state: CharacterState? = null,
        prefs: Preferences? = null,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<String> {
        val itemId = FloundryDatabase.itemIdForName(name)
            ?: return Result.failure(IllegalArgumentException("Unknown Floundry item: $name"))
        return purchase(itemId, state, prefs, accessibleCount)
    }

    companion object {
        const val FLOUNDRY_ITEM_CREATED_PREF = "_floundryItemCreated"

        internal fun preflight(
            name: String,
            itemId: Int,
            state: CharacterState?,
            prefs: Preferences?,
            accessibleCount: (Int) -> Int,
        ): Result<Unit> {
            if (ConcoctionDatabase.getByResult(name) == null) {
                return Result.failure(IllegalStateException("No concoction for: $name"))
            }
            if (state != null &&
                !ConcoctionMethodGates.isPermitted(
                    method = "FLOUNDRY",
                    state = state,
                    prefs = prefs,
                    accessibleCount = accessibleCount,
                )
            ) {
                return Result.failure(IllegalStateException("Floundry not permitted: $name"))
            }
            if (prefs != null && !ClanLoungeSync.hasFloundry(prefs)) {
                return Result.failure(IllegalStateException("Clan Floundry not available"))
            }
            if (itemId <= 0) {
                return Result.failure(IllegalStateException("Invalid Floundry item: $name"))
            }
            if (!FloundryAvailability.isAvailable(name)) {
                return Result.failure(IllegalStateException("Floundry item not available: $name"))
            }
            if (ConcoctionDatabase.totalCount(name) <= 0) {
                return Result.failure(IllegalStateException("Floundry item not creatable: $name"))
            }
            return Result.success(Unit)
        }
    }
}
