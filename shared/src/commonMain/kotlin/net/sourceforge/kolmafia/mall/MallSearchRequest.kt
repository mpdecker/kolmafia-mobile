package net.sourceforge.kolmafia.mall

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class MallSearchRequest(private val client: HttpClient) {

    suspend fun search(itemName: String, limit: Int): List<MallListing> {
        val results = mutableListOf<MallListing>()
        var start: Int? = null
        do {
            val html = submitSearch(itemName.trim(), limit, start) ?: return results
            results += parseMallHtml(html, if (limit <= 0) Int.MAX_VALUE else limit - results.size)
            val page = ITERATION_PATTERN.find(html)
            val end = page?.groupValues?.get(2)?.toIntOrNull()
            val total = page?.groupValues?.get(3)?.toIntOrNull()
            start = if (end != null && total != null && end < total) end else null
        } while (start != null && (limit <= 0 || results.size < limit))
        val trimmed = if (limit <= 0) results else results.take(limit)
        return MallSearchOverlay.merge(itemName, trimmed, if (limit <= 0) Int.MAX_VALUE else limit)
    }

    suspend fun searchStore(storeId: Int): List<MallListing> {
        val html = try {
            client.get("$KOL_BASE_URL/mallstore.php?whichstore=$storeId").bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return emptyList()
        }
        return parseStoreHtml(html)
    }

    suspend fun searchCategory(category: String, tiers: String = ""): List<MallListing> {
        val html = try {
            client.submitForm(
                url = "$KOL_BASE_URL/mall.php",
                formParameters = baseParameters("", 0, category = category, tiers = tiers),
            ).bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return emptyList()
        }
        return parseMallHtml(html, Int.MAX_VALUE)
    }

    private suspend fun submitSearch(itemName: String, limit: Int, start: Int?): String? = try {
        client.submitForm(
            url = "$KOL_BASE_URL/mall.php",
            formParameters = baseParameters(itemName, limit, start = start, action = "searchmall"),
        ).bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun baseParameters(
        search: String,
        limit: Int,
        start: Int? = null,
        action: String? = null,
        category: String = "allitems",
        tiers: String = "",
    ) = parameters {
        append("pudnuggler", search)
        append("category", category)
        append("consumable_byme", "0")
        append("weaponattribute", "3")
        append("wearable_byme", "0")
        append("nolimits", "0")
        append("max_price", "0")
        append("justitems", "0")
        append("sortresultsby", "price")
        append("x_cheapest", limit.toString())
        if (action != null) append("action", action)
        if (start != null) append("start", start.toString())
        if (tiers.isNotEmpty()) {
            for (tier in 1..5) {
                append("consumable_tier_$tier", if (tiers.contains(TIER_NAMES[tier - 1])) "1" else "0")
            }
        }
    }

    internal fun parseMallHtml(html: String, limit: Int): List<MallListing> {
        val desktopRows = parseDesktopMallHtml(html, limit)
        if (desktopRows.isNotEmpty()) return desktopRows

        val itemDetailRows = parseItemDetailMallHtml(html, limit)
        if (itemDetailRows.isNotEmpty()) return itemDetailRows

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

    private fun parseItemDetailMallHtml(html: String, limit: Int): List<MallListing> {
        val rows = mutableListOf<MallListing>()
        val storeListResult = html.substringAfter("Search Results:", html)
        ITEMDETAIL_PATTERN.findAll(storeListResult).forEach { itemMatch ->
            val itemId = itemMatch.groupValues[1].toIntOrNull() ?: return@forEach
            val descId = itemMatch.groupValues[2]
            val itemName = itemMatch.groupValues[3].trim()
            val dataName = ItemDatabase.getById(itemId)?.name
            if (dataName == null || !dataName.equals(itemName, ignoreCase = true)) {
                ItemDatabase.registerItem(itemId, itemName, descId)
            }
            val itemBody = itemMatch.groupValues[4]
            STOREDETAIL_PATTERN.findAll(itemBody).forEach rowLoop@{ row ->
                if (rows.size >= limit) return rows
                val linkText = row.value
                val quantity = LISTQUANTITY_PATTERN.find(linkText)?.groupValues?.get(1)
                    ?.replace(",", "")?.toIntOrNull() ?: 0
                var dailyLimit = quantity
                var canPurchase = true
                LISTLIMIT_PATTERN.find(linkText)?.let { limitMatch ->
                    dailyLimit = limitMatch.groupValues[1].replace(",", "").toIntOrNull() ?: quantity
                    canPurchase = !linkText.contains("graybelow limited", ignoreCase = true)
                }
                val detail = LISTDETAIL_PATTERN.find(linkText) ?: return@rowLoop
                rows += MallListing(
                    shopId = detail.groupValues[1].toInt(),
                    shopName = detail.groupValues[4]
                        .replace(Regex("""(?i)<br\s*/?>"""), " ")
                        .stripTags()
                        .trim(),
                    itemId = itemId,
                    price = detail.groupValues[3].toLong(),
                    quantity = quantity,
                    limit = minOf(quantity, dailyLimit),
                    canPurchase = canPurchase,
                )
            }
        }
        return rows
    }

    private fun parseDesktopMallHtml(html: String, limit: Int): List<MallListing> {
        val rows = mutableListOf<MallListing>()
        ITEM_TABLE_PATTERN.findAll(html.substringAfter("Search Results:", html)).forEach { itemMatch ->
            val itemBody = itemMatch.value
            val itemId = Regex("""item_(\d+)""").find(itemBody)?.groupValues?.get(1)?.toIntOrNull()
                ?: return@forEach
            STOREDETAIL_PATTERN.findAll(itemBody).forEach rowLoop@{ row ->
                if (rows.size >= limit) return rows
                val detail = LISTDETAIL_PATTERN.find(row.value) ?: return@rowLoop
                val quantity = LISTQUANTITY_PATTERN.find(row.value)?.groupValues?.get(1)
                    ?.replace(",", "")?.toIntOrNull() ?: 0
                val dailyLimit = LISTLIMIT_PATTERN.find(row.value)?.groupValues?.get(1)
                    ?.replace(",", "")?.toIntOrNull() ?: quantity
                rows += MallListing(
                    shopId = detail.groupValues[1].toInt(),
                    shopName = detail.groupValues[4].replace(Regex("""(?i)<br\s*/?>"""), " ").stripTags().trim(),
                    itemId = itemId,
                    price = detail.groupValues[3].toLong(),
                    quantity = quantity,
                    limit = minOf(quantity, dailyLimit),
                    canPurchase = !row.value.contains("graybelow limited", ignoreCase = true),
                )
            }
        }
        return rows
    }

    internal fun parseStoreHtml(html: String): List<MallListing> {
        val header = STORE_ID_PATTERN.find(html) ?: return emptyList()
        val shopName = header.groupValues[1].replace(Regex("""\s+;"""), ";").stripTags()
        val shopId = header.groupValues[2].toInt()
        return STORE_PRICE_PATTERN.findAll(html).mapNotNull { row ->
            val storeString = row.groupValues[1]
            val dot = storeString.indexOf('.')
            if (dot < 1) return@mapNotNull null
            val quantity = row.groupValues[3].replace(",", "").toIntOrNull() ?: return@mapNotNull null
            val dailyLimit = STORE_LIMIT_PATTERN.find(row.groupValues[4])?.groupValues?.get(1)
                ?.replace(",", "")?.toIntOrNull() ?: quantity
            MallListing(
                shopId, shopName, storeString.substring(0, dot).toInt(),
                storeString.substring(dot + 1).toLong(), quantity, minOf(quantity, dailyLimit),
            )
        }.toList()
    }

    private fun String.stripTags(): String = replace(Regex("<[^>]+>"), "")

    companion object {
        private val ITERATION_PATTERN = Regex("""\(Items ([\d,]+)-([\d,]+) of ([\d,]+)\)""")
        private val ITEMDETAIL_PATTERN = Regex(
            """<table class="itemtable".*?item_(\d+).*?descitem\((\d+)\).*?<a[^>]*>(.*?)</a>(.*?)</table>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val ITEM_TABLE_PATTERN = Regex(
            """<table class="itemtable".*?</table>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val STOREDETAIL_PATTERN = Regex(
            """<tr class="graybelow.*?</tr>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val LISTQUANTITY_PATTERN = Regex("""class="stock">([\d,]+)<""", RegexOption.IGNORE_CASE)
        private val LISTLIMIT_PATTERN = Regex("""([\d,]+)(?:&nbsp;|\s)*\/(?:&nbsp;|\s)*day""", RegexOption.IGNORE_CASE)
        private val LISTDETAIL_PATTERN = Regex(
            """whichstore=(\d+)&(?:amp;)?searchitem=(\d+)&(?:amp;)?searchprice=(\d+)"><b>(.*?)</b>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val STORE_ID_PATTERN = Regex("""<b style="color: [^"]+">(.*?) \(<a.*?who=(\d+)"""", RegexOption.DOT_MATCHES_ALL)
        private val STORE_PRICE_PATTERN = Regex(
            """radio value=([\d.]+).*?<b>(.*?)</b> \(([\d,]+)\)(.*?)</td>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val STORE_LIMIT_PATTERN = Regex("""Limit ([\d,]+) /""")
        private val TIER_NAMES = listOf("crappy", "decent", "good", "awesome", "EPIC")
    }
}
