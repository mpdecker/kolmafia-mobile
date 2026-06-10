package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class StorageRequest(private val client: HttpClient) {

    suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/storage.php") {
                parameter("action", "pullitem")
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

    /**
     * Fetches Hagnk's storage contents from api.php?what=storage.
     * Returns a map of item ID → quantity. Open so tests can override.
     */
    open suspend fun fetchContents(): Map<Int, Int> {
        return try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "storage")
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
