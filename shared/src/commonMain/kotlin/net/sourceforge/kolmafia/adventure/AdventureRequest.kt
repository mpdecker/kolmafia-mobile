package net.sourceforge.kolmafia.adventure

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class AdventureRequest(private val client: HttpClient) {
    // Returns Pair<responseBody, finalUrl>
    suspend fun adventure(location: AdventureLocation): Result<Pair<String, String>> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/adventure.php",
            formParameters = parameters {
                append("snarfblat", location.id)
                append("adv", "1")
            }
        )
        Result.success(response.bodyAsText() to response.request.url.toString())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
