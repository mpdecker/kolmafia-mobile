package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class UseItemRequest(private val client: HttpClient) {
    /**
     * Uses an item via inv_use.php.
     * @param itemId  KoL item ID
     * @param quantity  number to use (default 1)
     */
    open suspend fun use(itemId: Int, quantity: Int = 1): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/inv_use.php") {
                parameter("which", 3)
                parameter("whichitem", itemId)
                parameter("ajax", 1)
                if (quantity > 1) parameter("quantity", quantity)
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
}
