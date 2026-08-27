package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences

open class StorageRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
    private val character: KoLCharacter? = null,
    private val preferences: Preferences? = null,
) {

    open suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/storage.php") {
                parameter("action", "pullitem")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                TransferItemSync.parseStorageTransfer(
                    url = "storage.php?action=pullitem&whichitem=$itemId&qty=$quantity",
                    html = body,
                    itemId = itemId,
                    quantity = quantity,
                    inventory = inventoryManager,
                    character = character,
                    preferences = preferences,
                )
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun emptyStorage(): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/storage.php") {
                parameter("action", "pullall")
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop [StorageRequestType.PULL_MEAT_FROM_STORAGE] — storage.php?action=takemeat&amt=N. */
    open suspend fun pullMeat(quantity: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/storage.php") {
                parameter("action", "takemeat")
                parameter("amt", quantity)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                TransferItemSync.parseStorageTransfer(
                    url = "storage.php?action=takemeat&amt=$quantity",
                    html = body,
                    itemId = 0,
                    quantity = quantity,
                    inventory = inventoryManager,
                    character = character,
                    preferences = preferences,
                )
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun deposit(itemId: Int, quantity: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/storage.php") {
                parameter("action", "storeitem")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                TransferItemSync.parseStorageTransfer(
                    url = "storage.php?action=storeitem&whichitem=$itemId&qty=$quantity",
                    html = body,
                    itemId = itemId,
                    quantity = quantity,
                    inventory = inventoryManager,
                    character = character,
                    preferences = preferences,
                )
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Raw api.php?what=storage map. */
    protected open suspend fun fetchRawContents(): Map<Int, Int> {
        return try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "storage")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) return emptyMap()
            val rawMap: Map<String, Int> = response.body()
            rawMap.entries.mapNotNull { (k, v) -> k.toIntOrNull()?.to(v) }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    open suspend fun fetchClassifiedContents(
        characterState: CharacterState?,
        prefs: Preferences? = null,
    ): StoragePullRules.StorageContents =
        StoragePullRules.classifyContents(fetchRawContents(), characterState, prefs)

    open suspend fun fetchStorageContents(characterState: CharacterState?): Map<Int, Int> =
        fetchClassifiedContents(characterState).storage

    open suspend fun fetchFreepullContents(characterState: CharacterState?): Map<Int, Int> =
        fetchClassifiedContents(characterState).freepulls

    /**
     * Merged storage + freepull counts (backward compat for stash/context ID sets).
     */
    open suspend fun fetchContents(): Map<Int, Int> {
        val classified = fetchClassifiedContents(null)
        return mergeStorageMaps(classified.storage, classified.freepulls)
    }

    open suspend fun fetchContents(characterState: CharacterState?): Map<Int, Int> {
        val classified = fetchClassifiedContents(characterState)
        return mergeStorageMaps(classified.storage, classified.freepulls)
    }

    companion object {
        fun mergeStorageMaps(
            storage: Map<Int, Int>,
            freepulls: Map<Int, Int>,
        ): Map<Int, Int> {
            if (freepulls.isEmpty()) return storage
            val merged = storage.toMutableMap()
            for ((id, qty) in freepulls) {
                merged[id] = (merged[id] ?: 0) + qty
            }
            return merged
        }

        fun canUseStorage(characterState: CharacterState?): Boolean {
            val state = characterState ?: return true
            return StoragePullRules.canInteract(state) &&
                !LimitModeGates.limitStorage(state.limitMode)
        }
    }
}
