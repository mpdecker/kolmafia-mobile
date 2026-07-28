package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Desktop [TrendyRequest] — monthly trendy item availability on Trendy path. */
open class TrendyRequest(private val client: HttpClient) {

    companion object {
        private val trendy = mutableMapOf<RestrictedItemType, MutableMap<String, Boolean>>()
        private var initialized = false

        private val TRENDY_PATTERN = Regex(
            """<tr class="([^"]*)">.*?<td[^>]*>([^<]*)</td>.*?<td[^>]*>([^<]*)</td>.*?<td[^>]*>((?:[^<]*(?:(?!</t[dr]>)<))*[^<]*)</t[dr]>""",
            RegexOption.DOT_MATCHES_ALL,
        )

        fun resetForTest() {
            trendy.clear()
            initialized = false
        }

        fun parseResponse(html: String) {
            trendy.clear()
            for (match in TRENDY_PATTERN.findAll(html)) {
                val itemType = RestrictedItemType.fromString(match.groupValues[3].trim()) ?: continue
                val available = match.groupValues[1].trim() != "expired"
                val objects = match.groupValues[4]
                for (split in objects.split(", ")) {
                    val name = split.trim().lowercase()
                    if (name.isNotEmpty()) {
                        trendy.getOrPut(itemType) { mutableMapOf() }[name] = available
                    }
                }
            }
            initialized = true
        }

        /** True when trendy or unknown; false when listed as expired. */
        fun isTrendy(type: RestrictedItemType, key: String): Boolean {
            if (!initialized) return true
            val check = trendy[type] ?: return true
            val available = check[key.trim().lowercase()]
            return available == null || available
        }

        internal fun markInitialized() {
            initialized = true
        }

        internal fun isInitialized(): Boolean = initialized
    }

    open suspend fun refresh(): Result<Unit> {
        return try {
            val response = client.get("$KOL_BASE_URL/typeii.php")
            if (!response.status.isSuccess()) {
                markInitialized()
                Result.success(Unit)
            } else {
                parseResponse(response.bodyAsText())
                Result.success(Unit)
            }
        } catch (_: Exception) {
            markInitialized()
            Result.failure(Exception("Trendy refresh failed"))
        }
    }

    open suspend fun ensureInitialized() {
        if (!isInitialized()) {
            refresh()
        }
    }
}
