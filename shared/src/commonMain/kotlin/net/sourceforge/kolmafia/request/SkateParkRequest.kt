package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SkateParkAvailability

/** Desktop [SkateParkRequest] — daily Skate Park buffs. */
class SkateParkRequest(
    private val client: HttpClient,
) {
    suspend fun takeBuff(
        place: String,
        preferences: Preferences?,
    ): Result<String> {
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        val buffIndex = SkateParkAvailability.placeToBuff(place)
            ?: return Result.failure(
                IllegalArgumentException("That's not a valid location in the Skate Park"),
            )
        val data = SkateParkAvailability.buffToData(buffIndex)
            ?: return Result.failure(IllegalArgumentException("Unknown skate buff."))
        val status = prefs.getString("skateParkStatus", "war")
        if (status != data.state) {
            return Result.failure(IllegalStateException("You cannot visit ${data.place}."))
        }
        if (prefs.getBoolean(data.setting, false)) {
            return Result.failure(IllegalStateException(data.error))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/sea_skatepark.php") {
                parameter("action", data.action)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Skate Park visit failed."))
            }
            val html = response.bodyAsText()
            parseResponse(data.action, html, prefs)
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun parseResponse(action: String, html: String, preferences: Preferences?) {
            val prefs = preferences ?: return
            val data = SkateParkAvailability.BUFF_DATA.firstOrNull { it.action == action } ?: return
            when {
                html.contains("ocean/rumble") -> prefs.setString("skateParkStatus", "war")
                html.contains("ocean/ice_territory") -> prefs.setString("skateParkStatus", "ice")
                html.contains("ocean/roller_territory") ->
                    prefs.setString("skateParkStatus", "roller")
                html.contains("ocean/fountain") -> prefs.setString("skateParkStatus", "peace")
            }
            if (html.contains("You acquire an effect") || html.contains(data.error)) {
                prefs.setBoolean(data.setting, true)
            }
        }

        fun findBuffAction(place: String): String? {
            val buff = SkateParkAvailability.placeToBuff(place) ?: return null
            return SkateParkAvailability.buffToData(buff)?.action
        }
    }
}
