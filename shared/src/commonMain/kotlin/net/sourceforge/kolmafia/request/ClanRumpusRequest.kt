package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.clan.ClanLoungeVipSync
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

open class ClanRumpusRequest(private val client: HttpClient) {

    /** GETs clan_basement.php to collect rumpus room breakfast items. */
    open suspend fun visit(): Result<Unit> = try {
        val response = client.get("$KOL_BASE_URL/clan_basement.php")
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else
            Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Desktop ClanRumpusRequest(RequestType.BALLS) — jump in the awesome ball pit. */
    open suspend fun jumpInBallpit(preferences: Preferences? = null): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/clan_rumpus.php",
            formParameters = parameters {
                append("preaction", "ballpit")
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val html = response.bodyAsText()
            ClanLoungeVipSync.syncBallpitFromResponse(html, preferences)
            Result.success(html)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
