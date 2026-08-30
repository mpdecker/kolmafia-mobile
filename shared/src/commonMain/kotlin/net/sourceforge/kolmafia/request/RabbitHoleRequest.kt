package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Rabbit Hole place visit, including the redirecting Mad Tea Party action. */
class RabbitHoleRequest(private val client: HttpClient) {
    suspend fun visit(action: String? = null, preferences: Preferences? = null): Result<String> = try {
        val suffix = action?.let { "&action=$it" }.orEmpty()
        val response = client.get("$KOL_BASE_URL/place.php?whichplace=rabbithole$suffix")
        if (!response.status.isSuccess()) {
            Result.failure(IllegalStateException("Rabbit Hole visit failed: HTTP ${response.status.value}"))
        } else {
            val html = response.bodyAsText()
            parseResponse("place.php?whichplace=rabbithole$suffix", html, preferences)
            Result.success(html)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        fun parseResponse(url: String, responseText: String, preferences: Preferences?): Boolean {
            if (!url.contains("whichplace=rabbithole", true)) return false
            preferences?.setBoolean("rabbitHoleVisited", true)
            if (url.contains("action=rabbithole_teaparty", true) &&
                responseText.contains("already attended a Tea Party today", true)
            ) {
                preferences?.setBoolean("_madTeaParty", true)
            }
            return true
        }
    }
}
