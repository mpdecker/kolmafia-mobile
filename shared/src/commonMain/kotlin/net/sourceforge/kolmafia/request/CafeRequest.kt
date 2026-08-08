package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Desktop CafeRequest purchase HTTP — cafe.php CONSUME! */
open class CafeRequest(private val client: HttpClient) {

    open suspend fun consume(cafeId: String, whichItem: Int): Result<String> = try {
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
                else -> Result.success(html)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
