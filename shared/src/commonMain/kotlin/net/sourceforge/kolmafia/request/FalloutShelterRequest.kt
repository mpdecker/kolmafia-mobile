package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences

/** Minimal desktop [FalloutShelterRequest] — Nuclear Autumn fallout-shelter place visits. */
class FalloutShelterRequest(
    private val client: HttpClient,
) {
    suspend fun visitTerminal(): Result<String> = visitAction(VAULT_TERMINAL)

    suspend fun visitVault3(preferences: Preferences? = null): Result<String> =
        visitAction(VAULT3).onSuccess { html ->
            parseVault3Response(html, preferences)
        }

    internal suspend fun visitAction(action: String): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/place.php",
            formParameters = falloutShelterForm(action),
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            Result.success(response.bodyAsText())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        const val VAULT_TERMINAL = "vault_term"
        const val VAULT1 = "vault1"
        const val VAULT3 = "vault3"
        const val SPA_USED_PREF = "_falloutShelterSpaUsed"

        internal fun falloutShelterForm(action: String) = parameters {
            append("whichplace", "falloutshelter")
            append("action", action)
        }

        fun parseVault3Response(html: String, preferences: Preferences?) {
            if (preferences == null) return
            if (html.contains("entire day", ignoreCase = true)) {
                preferences.setBoolean(SPA_USED_PREF, true)
            }
        }

        fun preflightError(
            preferences: Preferences?,
            inNuclearAutumn: Boolean,
            limitMode: String,
        ): String? {
            if (!inNuclearAutumn) {
                return "Vault 3 is only available in Nuclear Autumn."
            }
            if (LimitModeGates.limitCampground(limitMode)) {
                return "You can't use the fallout shelter right now."
            }
            val level = preferences?.getInt("falloutShelterLevel", 0) ?: 0
            if (level < 3) {
                return "Your fallout shelter is not upgraded enough for Vault 3."
            }
            if (preferences?.getBoolean(SPA_USED_PREF, false) == true) {
                return "You have already used the Spa Simulation Chamber today."
            }
            return null
        }
    }
}
