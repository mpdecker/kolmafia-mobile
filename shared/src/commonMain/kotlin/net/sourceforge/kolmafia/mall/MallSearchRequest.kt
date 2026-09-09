package net.sourceforge.kolmafia.mall

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

class MallSearchRequest(private val client: HttpClient) {

    suspend fun search(itemName: String, limit: Int): List<MallListing> {
        val preflight = MallSearchPreflight.updateSearchString(itemName.trim())
        if (preflight.skipMallHttp) {
            return MallSearchOverlay.finalizeList(preflight.searchString, emptyList())
        }
        if (preflight.searchString.isEmpty()) {
            return searchFavorites(limit)
        }
        val results = searchInternal(preflight.searchString, limit)
        if (results.isNotEmpty()) return results
        val fuzzy = ItemDatabase.getMatchingNames(itemName.trim()).firstOrNull()
            ?: return results
        if (fuzzy.equals(itemName.trim(), ignoreCase = true)) return results
        val fuzzyPreflight = MallSearchPreflight.updateSearchString(fuzzy)
        if (fuzzyPreflight.skipMallHttp) {
            return MallSearchOverlay.finalizeList(fuzzyPreflight.searchString, emptyList())
        }
        return searchInternal(fuzzyPreflight.searchString, limit)
    }

    private suspend fun searchFavorites(limit: Int): List<MallListing> {
        val html = try {
            client.submitForm(
                url = "$KOL_BASE_URL/mall.php",
                formParameters = baseParameters("", 0),
            ).bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return emptyList()
        }
        val storeIds = FAVORITES_PATTERN.findAll(html)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .toList()
        if (storeIds.isEmpty()) return emptyList()
        val results = mutableListOf<MallListing>()
        for (storeId in storeIds) {
            if (limit > 0 && results.size >= limit) break
            results += searchStore(storeId)
        }
        return if (limit <= 0) results else results.take(limit)
    }

    private suspend fun searchInternal(itemName: String, limit: Int): List<MallListing> {
        val results = mutableListOf<MallListing>()
        var start: Int? = null
        do {
            val html = submitSearch(itemName, limit, start) ?: return MallSearchOverlay.finalizeList(itemName, results)
            results += parseMallHtml(html, if (limit <= 0) Int.MAX_VALUE else limit - results.size)
            val page = ITERATION_PATTERN.find(html)
            val end = page?.groupValues?.get(2)?.toIntOrNull()
            val total = page?.groupValues?.get(3)?.toIntOrNull()
            start = if (end != null && total != null && end < total) end else null
        } while (start != null && (limit <= 0 || results.size < limit))
        val trimmed = if (limit <= 0) results else results.take(limit)
        return MallSearchOverlay.finalizeList(itemName, trimmed)
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
        val cleaned = MallSearchHtmlPreprocessor.preprocess(html)
        val desktopRows = parseDesktopMallHtml(cleaned, limit)
        if (desktopRows.isNotEmpty()) return desktopRows

        val itemDetailRows = parseItemDetailMallHtml(cleaned, limit)
        if (itemDetailRows.isNotEmpty()) return itemDetailRows

        val storePattern = Regex("""mallstore\.php\?whichstore=(\d+)""")
        val itemPattern = Regex("""name="whichitem"\s+value="(\d+)"""")
        val pricePattern = Regex("""<b>(\d+)</b>\s*Meat""")
        val qtyPattern = Regex("""Quantity:\s*(\d+)""")

        val storeIds = storePattern.findAll(cleaned).map { it.groupValues[1].toInt() }.toList()
        val itemIds = itemPattern.findAll(cleaned).map { it.groupValues[1].toInt() }.toList()
        val prices = pricePattern.findAll(cleaned).map { it.groupValues[1].toLong() }.toList()
        val quantities = qtyPattern.findAll(cleaned).map { it.groupValues[1].toInt() }.toList()

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
        val shopName = MallSearchPreflight.decodeEntities(
            header.groupValues[1].replace(Regex("""\s+;"""), ";"),
        ).stripTags()
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
        internal val FAVORITES_PATTERN = Regex("""&action=unfave&whichstore=(\d+)">""")
        private val TIER_NAMES = listOf("crappy", "decent", "good", "awesome", "EPIC")

        /** Desktop MallSearchRequest.registerRequest session-log lines. */
        fun registerRequest(urlString: String, sessionLogger: SessionLogger?): Boolean {
            val url = urlString.substringAfterLast('/').substringBefore('#')
            val full = urlString.substringAfter("://").substringAfter('/').ifEmpty { urlString }
            val path = if (full.contains("mall")) full else url
            if (path.startsWith("mallstore.php")) {
                if (path.contains("buying=1") || path.contains("buying=Yep.")) return false
                val shopId = MallPurchaseRequest.getStoreId(path)
                val storeName = if (shopId != -1) "shop #$shopId" else "a PC store"
                RequestLogger.updateSessionLog("mallsearch $storeName", sessionLogger)
                return true
            }
            if (!path.startsWith("mall.php")) return false
            val message = buildString {
                append("mallsearch ")
                val search = decodeQueryParam(path, "pudnuggler")
                val category = decodeQueryParam(path, "category").ifEmpty { "allitems" }
                val start = decodeQueryParam(path, "start")
                val page = if (start.isEmpty()) 1 else ((start.toIntOrNull() ?: 0) / 30 + 1)
                if (search.isEmpty()) {
                    append("category ")
                    append(category)
                } else {
                    append(search)
                }
                if (page > 1) {
                    append(" (page ")
                    append(page)
                    append(")")
                }
            }
            RequestLogger.updateSessionLog(message, sessionLogger)
            return true
        }

        private fun decodeQueryParam(url: String, key: String): String {
            val raw = Regex("""(?:^|[?&])$key=([^&]*)""").find(url)?.groupValues?.get(1) ?: return ""
            return raw.replace("+", " ")
                .replace(Regex("%([0-9A-Fa-f]{2})")) { m ->
                    m.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: m.value
                }
        }
    }
}
