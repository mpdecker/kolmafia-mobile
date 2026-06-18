package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.VolcanoMazeManager
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants

open class VolcanoMazeRequest(private val client: HttpClient) {

    suspend fun visit(): Result<String> = fetch("volcanomaze.php?start=1")

    suspend fun jump(): Result<String> = fetch("volcanomaze.php?jump=1")

    suspend fun move(pos: Int): Result<String> {
        val row = VolcanoMazeConstants.row(pos)
        val col = VolcanoMazeConstants.col(pos)
        return move(col, row)
    }

    suspend fun move(col: Int, row: Int): Result<String> =
        fetch(getMoveUrl(col, row))

    suspend fun fetch(path: String): Result<String> = try {
        val normalized = path.removePrefix("/")
        val response = client.get("$KOL_BASE_URL/$normalized")
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            Result.success(response.bodyAsText())
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    open fun handleResponse(
        url: String,
        body: String,
        preferences: Preferences,
    ): String? {
        if (body.contains("A niggling voice")) {
            return "Equip your Legendary Epic Weapon and try again."
        }
        if (url.contains("volcanomaze.php")) {
            VolcanoMazeManager.parseResult(preferences, body)
        }
        return null
    }

    suspend fun runFetch(
        path: String,
        preferences: Preferences,
    ): Result<String> {
        val url = path.removePrefix("/")
        return fetch(url).fold(
            onSuccess = { body ->
                val error = handleResponse(url, body, preferences)
                if (error != null) Result.failure(Exception(error)) else Result.success(body)
            },
            onFailure = { Result.failure(it) },
        )
    }

    companion object {
        fun getMoveUrl(col: Int, row: Int): String =
            "volcanomaze.php?move=$col,$row&ajax=1"

        fun getMoveUrl(pos: Int): String {
            val row = VolcanoMazeConstants.row(pos)
            val col = VolcanoMazeConstants.col(pos)
            return getMoveUrl(col, row)
        }
    }
}
