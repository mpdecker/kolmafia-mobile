package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class OceanRequest(private val client: HttpClient) {

    suspend fun sail(lon: Int, lat: Int): Result<Pair<String, String>> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/ocean.php",
            formParameters = parameters {
                append("lon", lon.toString())
                append("lat", lat.toString())
            },
        )
        Result.success(response.bodyAsText() to response.request.url.toString())
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private val lonInputPattern =
            Regex("""<input type=text class=text size=5 name=lon""", RegexOption.IGNORE_CASE)
        private val latInputPattern =
            Regex("""<input type=text class=text size=5 name=lat""", RegexOption.IGNORE_CASE)

        fun isOceanPage(html: String, url: String): Boolean {
            if (url.contains("ocean.php", ignoreCase = true)) return true
            return lonInputPattern.containsMatchIn(html) && latInputPattern.containsMatchIn(html)
        }
    }
}
