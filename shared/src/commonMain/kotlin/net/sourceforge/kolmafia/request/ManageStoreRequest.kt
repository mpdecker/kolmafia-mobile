package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class ManageStoreRequest(private val client: HttpClient) {

    open suspend fun addItem(
        itemId: Int,
        price: Int,
        limit: Int = 0,
        quantity: Int = 1,
        fromStorage: Boolean = false,
    ): Result<String> = submitBackoffice(
        parameters = Parameters.build {
            append("action", "additem")
            append("ajax", "1")
            append("itemid", if (fromStorage) "h$itemId" else itemId.toString())
            append("price", price.toString())
            append("quantity", quantity.toString())
            if (limit > 0) append("limit", limit.toString())
        }
    )

    open suspend fun removeItem(itemId: Int, quantity: Int): Result<String> = submitBackoffice(
        parameters = Parameters.build {
            append("action", "removeitem")
            append("ajax", "1")
            append("itemid", itemId.toString())
            append("qty", quantity.toString())
        }
    )

    open suspend fun repriceItem(itemId: Int, price: Int, limit: Int = 0): Result<String> =
        submitBackoffice(
            parameters = Parameters.build {
                append("action", "updateinv")
                append("ajax", "1")
                append("price[$itemId]", price.toString())
                if (limit > 0) append("limit[$itemId]", limit.toString())
            }
        )

    open suspend fun refreshPrices(): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/manageprices.php",
            formParameters = Parameters.build { append("action", "refresh") }
        )
        if (response.status.isSuccess()) Result.success(response.bodyAsText())
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun submitBackoffice(parameters: Parameters): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/backoffice.php",
            formParameters = parameters,
        )
        if (response.status.isSuccess()) Result.success(response.bodyAsText())
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
