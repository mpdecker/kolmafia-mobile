package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Desktop [ThriftyRequest] — daily allowed items/skills/familiars on Thrifty path. */
open class ThriftyRequest(private val client: HttpClient) {

    companion object {
        private val allowed = mutableMapOf<RestrictedItemType, MutableSet<String>>()
        private var initialized = false

        fun resetForTest() {
            allowed.clear()
            initialized = false
        }

        fun parseResponse(html: String) {
            allowed.clear()
            for ((type, names) in RestrictedItemsParse.parseSections(html)) {
                allowed.getOrPut(type) { mutableSetOf() }.addAll(names)
            }
            initialized = true
        }

        fun isAllowed(type: RestrictedItemType, key: String): Boolean {
            if (!initialized) return false
            return allowed[type]?.contains(key.trim().lowercase()) == true
        }

        internal fun markInitialized() {
            initialized = true
        }

        internal fun isInitialized(): Boolean = initialized
    }

    open suspend fun refresh(): Result<Unit> {
        return try {
            val response = client.get("$KOL_BASE_URL/thrifty.php")
            if (!response.status.isSuccess()) {
                markInitialized()
                Result.success(Unit)
            } else {
                parseResponse(response.bodyAsText())
                Result.success(Unit)
            }
        } catch (_: Exception) {
            markInitialized()
            Result.failure(Exception("Thrifty refresh failed"))
        }
    }

    open suspend fun ensureInitialized() {
        if (!isInitialized()) {
            refresh()
        }
    }
}
