package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Minimal desktop [FalloutShelterRequest] — Nuclear Autumn fallout-shelter place visits. */
class FalloutShelterRequest(
    private val client: HttpClient,
) {
    suspend fun visitTerminal(): Result<String> = visitAction(VAULT_TERMINAL)

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

        internal fun falloutShelterForm(action: String) = parameters {
            append("whichplace", "falloutshelter")
            append("action", action)
        }
    }
}
