package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class ClanStashRequest(private val client: HttpClient) {

    /** Contribute [quantity] of item [itemId] to the clan stash. */
    open suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/clan_stash.php") {
                parameter("action", "contribute")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) Result.success(response.bodyAsText())
            else Result.failure(Exception("HTTP ${response.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Take [quantity] of item [itemId] from the clan stash into the backpack. */
    open suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/clan_stash.php") {
                parameter("action", "take")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) Result.success(response.bodyAsText())
            else Result.failure(Exception("HTTP ${response.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
