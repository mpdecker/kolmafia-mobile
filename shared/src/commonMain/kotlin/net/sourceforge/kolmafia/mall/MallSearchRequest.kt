package net.sourceforge.kolmafia.mall

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class MallSearchRequest(private val client: HttpClient) {

    suspend fun search(itemName: String, limit: Int): List<MallListing> {
        val html = try {
            client.submitForm(
                url = "$KOL_BASE_URL/mall.php",
                formParameters = parameters {
                    append("pudnuggler", itemName)
                    append("justitems", "0")
                    append("sortresultsby", "price")
                    append("action", "searchmall")
                }
            ).bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return emptyList()
        }
        return parseMallHtml(html, limit)
    }

    private fun parseMallHtml(html: String, limit: Int): List<MallListing> {
        val storePattern = Regex("""mallstore\.php\?whichstore=(\d+)""")
        val itemPattern = Regex("""name="whichitem"\s+value="(\d+)"""")
        val pricePattern = Regex("""<b>(\d+)</b>\s*Meat""")
        val qtyPattern = Regex("""Quantity:\s*(\d+)""")

        val storeIds = storePattern.findAll(html).map { it.groupValues[1].toInt() }.toList()
        val itemIds = itemPattern.findAll(html).map { it.groupValues[1].toInt() }.toList()
        val prices = pricePattern.findAll(html).map { it.groupValues[1].toLong() }.toList()
        val quantities = qtyPattern.findAll(html).map { it.groupValues[1].toInt() }.toList()

        return (0 until minOf(storeIds.size, limit)).mapNotNull { i ->
            val shopId = storeIds.getOrNull(i) ?: return@mapNotNull null
            val price = prices.getOrNull(i) ?: return@mapNotNull null
            val qty = quantities.getOrNull(i) ?: 0
            val itemId = itemIds.getOrNull(i) ?: 0
            MallListing(shopId = shopId, shopName = "", itemId = itemId,
                price = price, quantity = qty)
        }
    }
}
