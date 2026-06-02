package net.sourceforge.kolmafia.adventure

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class FightRequest(private val client: HttpClient) {
    suspend fun fight(macroText: String): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/fight.php",
            formParameters = parameters {
                append("action", "macro")
                append("macrotext", macroText)
            }
        )
        Result.success(response.bodyAsText())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
