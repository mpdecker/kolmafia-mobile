package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.modifiers.StringModifier

/** Desktop [StandardRequest] — hardcore/softcore restricted item lists. */
open class StandardRequest(private val client: HttpClient) {

    companion object {
        private val restricted = mutableMapOf<RestrictedItemType, MutableSet<String>>()
        private var initialized = false

        fun resetForTest() {
            restricted.clear()
            initialized = false
        }

        fun parseResponse(html: String) {
            restricted.clear()
            for ((type, names) in RestrictedItemsParse.parseSections(html)) {
                restricted.getOrPut(type) { mutableSetOf() }.addAll(names)
            }
            initialized = true
        }

        fun isNotRestricted(
            type: RestrictedItemType,
            key: String,
            state: CharacterState? = null,
        ): Boolean {
            if (state?.isRestricted != true) return true
            if (!initialized) return true
            return restricted[type]?.contains(key.trim().lowercase()) != true
        }

        fun isAllowedInStandard(
            type: RestrictedItemType,
            key: String,
            state: CharacterState? = null,
        ): Boolean {
            if (type == RestrictedItemType.BOOKSHELF_BOOKS) {
                return isNotRestricted(RestrictedItemType.BOOKSHELF_BOOKS, key, state) &&
                    isNotRestricted(RestrictedItemType.ITEMS, key, state)
            }
            return isNotRestricted(type, key, state)
        }

        fun isAllowed(
            type: RestrictedItemType,
            key: String,
            state: CharacterState?,
        ): Boolean {
            if (state == null) return true
            if (state.isTrendy && !TrendyRequest.isTrendy(type, key)) {
                return false
            }
            if (state.isThrifty) {
                if (type == RestrictedItemType.FAMILIARS &&
                    !ThriftyRequest.isAllowed(type, key)
                ) {
                    return false
                }
                if (type == RestrictedItemType.SKILLS &&
                    !ThriftyRequest.isAllowed(type, key)
                ) {
                    return false
                }
                if (type == RestrictedItemType.ITEMS &&
                    ModifierDatabase.getStringModifier(key, StringModifier.LAST_AVAILABLE_DATE)
                        .isNotBlank()
                ) {
                    return false
                }
            }
            if (state.inQuantum && type == RestrictedItemType.FAMILIARS) {
                return true
            }
            if (!state.isRestricted) {
                return true
            }
            return isAllowedInStandard(type, key, state)
        }

        internal fun markInitialized() {
            initialized = true
        }

        internal fun isInitialized(): Boolean = initialized

        fun defaultDateParam(): String = "2024-01-02"
    }

    open suspend fun refresh(date: String = defaultDateParam()): Result<Unit> {
        return try {
            val response = client.get("$KOL_BASE_URL/standard.php") {
                parameter("date", date)
            }
            if (!response.status.isSuccess()) {
                markInitialized()
                Result.success(Unit)
            } else {
                parseResponse(response.bodyAsText())
                Result.success(Unit)
            }
        } catch (_: Exception) {
            markInitialized()
            Result.failure(Exception("Standard refresh failed"))
        }
    }

    open suspend fun ensureInitialized() {
        if (!isInitialized()) {
            refresh()
        }
    }
}
