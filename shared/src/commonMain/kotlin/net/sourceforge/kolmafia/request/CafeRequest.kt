package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/** Desktop CafeRequest purchase HTTP — cafe.php CONSUME! / menu visit. */
open class CafeRequest(private val client: HttpClient) {

    /** Desktop cafe.php ResponseTextParser / visit hook — daily special + consume side effects. */
    companion object {
        fun parseResponse(
            urlString: String,
            responseText: String,
            preferences: Preferences?,
            inventory: InventoryManager? = ResultProcessor.inventoryProvider?.invoke(),
            character: KoLCharacter? = null,
        ) {
            CafeDailySpecialSync.parseResponse(urlString, responseText, preferences)
            MicroBreweryRequest.parseResponse(urlString, responseText, preferences)
            ChezSnooteeRequest.parseResponse(
                urlString,
                responseText,
                preferences,
                inventory,
                character,
            )
        }
    }

    /** Desktop cafe.php?cafeid=N visit — seeds Today's Special via [CafeDailySpecialSync]. */
    open suspend fun visitMenu(cafeId: String, preferences: Preferences?): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/cafe.php",
            formParameters = parameters {
                append("cafeid", cafeId)
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val html = response.bodyAsText()
            CafeDailySpecialSync.parseResponse("cafe.php?cafeid=$cafeId", html, preferences)
            Result.success(html)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun consume(
        cafeId: String,
        whichItem: Int,
        preferences: Preferences? = null,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
    ): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/cafe.php",
            formParameters = parameters {
                append("cafeid", cafeId)
                append("action", "CONSUME!")
                append("whichitem", whichItem.toString())
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val html = response.bodyAsText()
            when {
                html.contains("You can't afford that item.") ->
                    Result.failure(IllegalStateException("Insufficient funds"))
                html.contains("You're way too drunk already.") ||
                    html.contains("You're too full to eat that.") ->
                    Result.failure(IllegalStateException("Consumption limit reached"))
                html.contains("This is not currently available to you.") ->
                    Result.failure(IllegalStateException("Cafe item not available"))
                else -> {
                    parseResponse(
                        "cafe.php?cafeid=$cafeId&action=CONSUME!&whichitem=$whichItem",
                        html,
                        preferences,
                        inventory,
                        character,
                    )
                    Result.success(html)
                }
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
