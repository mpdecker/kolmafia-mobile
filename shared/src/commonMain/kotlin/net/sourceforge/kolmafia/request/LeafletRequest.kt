package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Executes one command in the Strange Leaflet text adventure. */
class LeafletRequest(private val client: HttpClient) {
    suspend fun execute(command: String): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/leaflet.php",
            formParameters = parameters { append("command", command) },
        )
        if (!response.status.isSuccess()) {
            Result.failure(IllegalStateException("Leaflet command failed: HTTP ${response.status.value}"))
        } else {
            Result.success(response.bodyAsText())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private val responsePattern = Regex("""<td><b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
        private val mantelPattern = Regex("""A ([a-z ]*?) sits on the mantelpiece""", RegexOption.IGNORE_CASE)

        fun parseMantelpieceLog(responseText: String): String? {
            val title = responsePattern.find(responseText)?.groupValues?.get(1) ?: return null
            val objectName = mantelPattern.find(title)?.groupValues?.get(1) ?: return null
            return "(You see a $objectName)"
        }
    }
}
