package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop BasementRequest.checkBasement level sync (Phases 2301–2315).
 */
object BasementSync {
    private val LEVEL = Regex("""Level ([\d,]+)""", RegexOption.IGNORE_CASE)

    @Volatile
    var basementLevel: Int = 0

    fun parseLevel(html: String): Int? =
        LEVEL.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()

    fun checkBasement(
        html: String,
        preferences: Preferences? = null,
        autoSwitch: Boolean = false,
    ): Int {
        val level = parseLevel(html) ?: return basementLevel
        basementLevel = level
        preferences?.setInt("basementLevel", level)
        if (autoSwitch) {
            preferences?.setBoolean("_basementAutoChecked", true)
        }
        return level
    }

    fun resetForTest() {
        basementLevel = 0
    }
}

open class BasementRequest(private val client: HttpClient) {
    open suspend fun visit(preferences: Preferences? = null): Result<Int> = try {
        val response = client.get("$KOL_BASE_URL/basement.php")
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val html = response.bodyAsText()
            Result.success(BasementSync.checkBasement(html, preferences))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
