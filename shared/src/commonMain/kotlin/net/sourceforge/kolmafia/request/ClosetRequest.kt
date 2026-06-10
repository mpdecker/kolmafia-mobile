package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class ClosetRequest(private val client: HttpClient) {

    open suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/closet.php") {
                parameter("action", "put")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/closet.php") {
                parameter("action", "take")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Take all items from the closet into inventory. */
    open suspend fun emptyCloset(): Result<Int> {
        return try {
            val contents = fetchContents()
            var moved = 0
            for ((itemId, qty) in contents) {
                if (takeOut(itemId, qty).isSuccess) moved += qty
            }
            Result.success(moved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the closet contents from api.php?what=closet.
     * Returns a map of item ID → quantity. Open so tests can override.
     */
    open suspend fun fetchContents(): Map<Int, Int> {
        return try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "closet")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) return emptyMap()
            val rawMap: Map<String, Int> = response.body()
            rawMap.entries.mapNotNull { (k, v) -> k.toIntOrNull()?.to(v) }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
