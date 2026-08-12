package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.IslandWarConcert

/**
 * Desktop [IslandRequest] concert / nunnery GET runners.
 * Uses query-string GETs matching [BreakfastManager] island sidequest visits.
 */
class IslandWarRequest(
    private val client: HttpClient,
) {
    suspend fun runConcert(option: Int, preferences: Preferences): Result<Pair<String, String>> {
        val path = IslandWarConcert.concertUrl(option, preferences)
            ?: return Result.failure(IllegalStateException("Mysterious Island is not available."))
        return getIsland(path)
    }

    suspend fun runNunnery(preferences: Preferences): Result<Pair<String, String>> {
        val path = IslandWarConcert.nunneryUrl(preferences)
            ?: return Result.failure(IllegalStateException("Mysterious Island is not available."))
        return getIsland(path)
    }

    private suspend fun getIsland(path: String): Result<Pair<String, String>> {
        return try {
            val url = "$KOL_BASE_URL/$path"
            val response = client.get(url)
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Island request failed."))
            }
            Result.success(response.bodyAsText() to url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
