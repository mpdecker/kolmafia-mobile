package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/**
 * Wraps hermit.php trade requests.
 * POST hermit.php?action=trade&whichitem=ID&quantity=N
 */
class HermitRequest(private val client: HttpClient) {
    suspend fun trade(itemId: Int, quantity: Int): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/hermit.php",
            formParameters = Parameters.build {
                append("action",    "trade")
                append("whichitem", itemId.toString())
                append("quantity",  quantity.toString())
            }
        )
        if (response.status.isSuccess()) {
            Result.success(response.bodyAsText())
        } else {
            Result.failure(Exception("HTTP ${response.status.value}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
