package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class UneffectRequest(private val client: HttpClient) {

    /** POSTs to uneffect.php to remove the effect with the given server ID. */
    open suspend fun uneffect(effectId: Int): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/uneffect.php",
            formParameters = parameters {
                append("using", "Yep.")
                append("whicheffect", effectId.toString())
            }
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
