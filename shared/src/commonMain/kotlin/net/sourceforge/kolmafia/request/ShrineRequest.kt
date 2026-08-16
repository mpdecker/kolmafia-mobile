package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ShrineRequest] — Hall of Legends meat donation. */
open class ShrineRequest(private val client: HttpClient) {

    data class Shrine(
        val id: Int,
        val action: String,
        val place: String,
        val setting: String,
    )

    open suspend fun donate(
        heroId: Int,
        amount: Int,
        preferences: Preferences?,
        hasStatueKey: Boolean = true,
    ): Result<String> {
        if (!hasStatueKey) {
            return Result.failure(IllegalStateException("You don't have the appropriate key."))
        }
        val data = idToData(heroId)
            ?: return Result.failure(IllegalStateException("Unknown shrine."))
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/da.php",
                formParameters = parameters {
                    append("action", data.action)
                    append("howmuch", amount.toString())
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("HTTP ${response.status.value}"))
            }
            val html = response.bodyAsText()
            val error = parseResponse(data.action, amount, html, preferences)
            if (error != null) Result.failure(IllegalStateException(error))
            else Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val BORIS = 1
        const val JARLSBERG = 2
        const val PETE = 3

        val SHRINE_DATA = listOf(
            Shrine(BORIS, "boris", "Statue of Boris", "heroDonationBoris"),
            Shrine(JARLSBERG, "jarlsberg", "Statue of Jarlsberg", "heroDonationJarlsberg"),
            Shrine(PETE, "sneakypete", "Statue of Sneaky Pete", "heroDonationSneakyPete"),
        )

        fun idToData(id: Int): Shrine? = SHRINE_DATA.firstOrNull { it.id == id }

        fun parseResponse(
            action: String,
            qty: Int,
            responseText: String,
            preferences: Preferences?,
        ): String? {
            val data = SHRINE_DATA.firstOrNull { it.action == action } ?: return null
            if (responseText.contains("bgshrine.gif")) {
                preferences?.setBoolean("barrelShrineUnlocked", true)
                if (responseText.contains("already prayed to the Barrel god")) {
                    preferences?.setBoolean("_barrelPrayer", true)
                }
            }
            if (!responseText.contains("You gain")) {
                return if (!responseText.contains("That's not enough")) {
                    "Donation limit exceeded."
                } else {
                    "Donation must be larger."
                }
            }
            val prev = preferences?.getInt(data.setting, 0) ?: 0
            preferences?.setInt(data.setting, prev + qty)
            return null
        }
    }
}
