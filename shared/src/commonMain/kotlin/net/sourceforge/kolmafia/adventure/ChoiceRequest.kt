package net.sourceforge.kolmafia.adventure

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class ChoiceRequest(private val client: HttpClient) {
    suspend fun choose(choiceId: Int, option: Int): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/choice.php",
            formParameters = parameters {
                append("whichchoice", choiceId.toString())
                append("option", option.toString())
            }
        )
        Result.success(response.bodyAsText())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
