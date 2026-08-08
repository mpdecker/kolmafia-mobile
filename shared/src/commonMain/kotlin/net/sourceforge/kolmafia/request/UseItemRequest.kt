package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
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

    /** Desktop MultiUseRequest — multi-use an ingredient stack via multiuse.php. */
    open suspend fun multiUse(itemId: Int, quantity: Int): Result<String> {
        if (quantity <= 0) return Result.success("")
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/multiuse.php",
                formParameters = parameters {
                    append("action", "useitem")
                    append("whichitem", itemId.toString())
                    append("quantity", quantity.toString())
                },
            )
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop UseItemRequest GLUTTONOUS_GHOST / SPIRIT_HOBO / SLIMELING binge via familiarbinger.php. */
    open suspend fun binge(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/familiarbinger.php") {
                parameter("whichitem", itemId)
                parameter("action", "binge")
                parameter("qty", quantity)
            }
            parseFamiliarFeedResponse(response.status.isSuccess(), response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop UseItemRequest STOCKING_MIMIC candy feed via familiarbinger.php. */
    open suspend fun feedCandy(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/familiarbinger.php") {
                parameter("whichitem", itemId)
                parameter("action", "candy")
                parameter("qty", quantity)
            }
            parseFamiliarFeedResponse(response.status.isSuccess(), response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop UseItemRequest ROBORTENDER robooze via inventory.php (qty 1 per call). */
    open suspend fun robooze(itemId: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/inventory.php") {
                parameter("action", "robooze")
                parameter("whichitem", itemId)
                parameter("ajax", 1)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val body = response.bodyAsText()
            if (body.contains("can't drink that", ignoreCase = true)) {
                Result.failure(IllegalStateException("Your Robortender can't drink that."))
            } else {
                Result.success(body)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFamiliarFeedResponse(httpSuccess: Boolean, body: String): Result<String> {
        if (!httpSuccess) {
            return Result.failure(Exception("HTTP request failed"))
        }
        if (body.contains("don't currently have", ignoreCase = true) ||
            body.contains("not currently using", ignoreCase = true)
        ) {
            return Result.failure(IllegalStateException("Your current familiar can't use that."))
        }
        return Result.success(body)
    }
}
