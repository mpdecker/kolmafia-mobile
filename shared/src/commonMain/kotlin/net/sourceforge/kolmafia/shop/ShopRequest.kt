package net.sourceforge.kolmafia.shop

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class ShopRequest(private val client: HttpClient) {

    suspend fun buy(shopId: String, rowId: Int, quantity: Int): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/shop.php",
            formParameters = parameters {
                append("whichshop", shopId)
                append("action", "buyitem")
                append("whichrow", rowId.toString())
                append("quantity", quantity.toString())
                append("ajax", "1")
            }
        )
        Result.success(response.bodyAsText())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
