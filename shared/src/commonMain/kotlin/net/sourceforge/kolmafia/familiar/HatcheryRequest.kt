package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class HatcheryRequest(private val client: HttpClient) {
    suspend fun hatch(eggItemId: Int): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/hatchery.php",
            formParameters = parameters {
                append("action", "hatch")
                append("whichitem", eggItemId.toString())
            }
        )
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
