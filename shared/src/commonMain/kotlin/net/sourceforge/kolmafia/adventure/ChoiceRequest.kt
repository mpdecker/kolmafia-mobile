package net.sourceforge.kolmafia.adventure

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class ChoiceRequest(private val client: HttpClient) {
    suspend fun choose(
        choiceId: Int,
        option: Int,
        extraFormFields: Map<String, String> = emptyMap(),
    ): Result<Pair<String, String>> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/choice.php",
            formParameters = parameters {
                append("whichchoice", choiceId.toString())
                append("option", option.toString())
                for ((key, value) in extraFormFields) {
                    append(key, value)
                }
            }
        )
        if (response.status.isSuccess()) {
            Result.success(response.bodyAsText() to response.request.url.toString())
        } else {
            Result.failure(Exception("HTTP ${response.status.value}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
