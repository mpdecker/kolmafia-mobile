package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

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
}
