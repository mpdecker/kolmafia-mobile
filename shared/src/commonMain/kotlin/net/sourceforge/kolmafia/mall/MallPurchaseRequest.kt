package net.sourceforge.kolmafia.mall

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class MallPurchaseRequest(private val client: HttpClient) {

    suspend fun buy(shopId: Int, itemId: Int, quantity: Int, price: Long): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/mall.php",
            formParameters = parameters {
                append("whichstore", shopId.toString())
                append("itemid", itemId.toString())
                append("quantity", quantity.toString())
                append("price", price.toString())
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
