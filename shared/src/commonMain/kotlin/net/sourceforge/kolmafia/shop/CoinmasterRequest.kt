package net.sourceforge.kolmafia.shop

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class CoinmasterRequest(private val client: HttpClient) {

    suspend fun buy(coinmaster: CoinmasterData, rowId: Int, quantity: Int): Result<String> =
        if (coinmaster.useItemField) {
            submitItemField(coinmaster, coinmaster.buyUrl, coinmaster.buyAction, rowId, quantity)
        } else {
            submitShopRow(coinmaster, "buyitem", rowId, quantity)
        }

    suspend fun sell(coinmaster: CoinmasterData, rowId: Int, quantity: Int): Result<String> =
        if (coinmaster.useItemField) {
            submitItemField(coinmaster, coinmaster.sellUrl, coinmaster.sellAction, rowId, quantity)
        } else {
            submitShopRow(coinmaster, "sellitem", rowId, quantity)
        }

    private suspend fun submitShopRow(
        coinmaster: CoinmasterData,
        action: String,
        rowId: Int,
        quantity: Int
    ): Result<String> {
        val shopId = coinmaster.shopId
            ?: return Result.failure(IllegalArgumentException("${coinmaster.masterName} has no shopId"))
        return submitForm(
            url = "$KOL_BASE_URL/shop.php",
            parameters = parameters {
                append("whichshop", shopId)
                append("action", action)
                append("whichrow", rowId.toString())
                append("quantity", quantity.toString())
                append("ajax", "1")
            }
        )
    }

    private suspend fun submitItemField(
        coinmaster: CoinmasterData,
        path: String?,
        action: String,
        itemId: Int,
        quantity: Int
    ): Result<String> {
        val urlPath = path?.trimStart('/')
            ?: return Result.failure(IllegalArgumentException("${coinmaster.masterName} has no buy/sell URL"))
        return submitForm(
            url = "$KOL_BASE_URL/$urlPath",
            parameters = parameters {
                append("action", action)
                append("whichitem", itemId.toString())
                append("howmany", quantity.toString())
                append("ajax", "1")
            }
        )
    }

    private suspend fun submitForm(url: String, parameters: Parameters): Result<String> {
        return try {
            val response = client.submitForm(url = url, formParameters = parameters)
            Result.success(response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
