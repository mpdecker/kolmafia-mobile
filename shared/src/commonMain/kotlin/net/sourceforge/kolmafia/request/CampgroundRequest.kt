package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class CampgroundRequest(private val client: HttpClient) {

    /** POSTs campground.php?action=garden. Best-effort: success does not guarantee items existed. */
    open suspend fun harvestGarden(): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/campground.php",
            formParameters = parameters {
                append("action", "garden")
            }
        )
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else
            Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** POSTs campground.php?action=spinningwheel — uses the workshed spinning wheel. */
    open suspend fun useSpinningWheel(): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/campground.php",
            formParameters = parameters {
                append("action", "spinningwheel")
            }
        )
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else
            Result.success(response.bodyAsText())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
