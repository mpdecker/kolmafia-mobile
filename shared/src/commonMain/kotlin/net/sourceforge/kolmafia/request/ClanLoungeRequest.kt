package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class ClanLoungeRequest(private val client: HttpClient) {

    /** Use the Deluxe Klaw machine once. Returns response HTML (caller checks _deluxeKlawSummons). */
    open suspend fun useKlaw(): Result<String> = postAction("klaw")

    /** Visit the looking glass for a free buff. */
    open suspend fun useLookingGlass(): Result<Unit> = postAction("lookingglass").map {}

    /** Visit the fireworks shop. */
    open suspend fun visitFireworks(): Result<Unit> = postAction("fireworks").map {}

    /** Play one pool game. */
    open suspend fun playPoolGame(): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/clan_viplounge.php",
            formParameters = parameters {
                append("preaction", "poolgame")
                append("action", "pooltable")
            }
        )
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else
            Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun postAction(action: String): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/clan_viplounge.php",
            formParameters = parameters { append("action", action) }
        )
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else
            Result.success(response.bodyAsText())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
