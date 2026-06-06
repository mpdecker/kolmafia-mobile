package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class UneffectRequest(private val client: HttpClient) {

    /** POSTs to uneffect.php to remove the effect with the given server ID. */
    suspend fun uneffect(effectId: Int): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/uneffect.php",
            formParameters = parameters {
                append("using", "Yep.")
                append("whicheffect", effectId.toString())
            }
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
