package net.sourceforge.kolmafia.inventory

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.character.KoLCharacter

open class InventoryManager(
    private val client: HttpClient,
    private val eventBus: GameEventBus,
    private val characterRequest: CharacterRequest? = null,
    private val character: KoLCharacter? = null,
) {
    private val _state = MutableStateFlow(InventoryState())
    open val state: StateFlow<InventoryState> = _state.asStateFlow()

    fun initialize(scope: CoroutineScope) {
        scope.launch {
            fetchInventory()
            eventBus.events.collect { event ->
                when (event) {
                    is GameEvent.ItemObtained, is GameEvent.ItemConsumed,
                    is GameEvent.ItemEquipped, is GameEvent.ItemDiscarded,
                    is GameEvent.ItemCrafted, is GameEvent.MallPurchase -> fetchInventory()
                    else -> {}
                }
            }
        }
    }

    open suspend fun fetchInventory() {
        try {
            val invResponse = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "inventory")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!invResponse.status.isSuccess()) {
                _state.value = _state.value.copy(isStale = true)
                return
            }
            // api.php?what=inventory returns {"itemId": quantity, ...}
            // Verify actual response format against live KoL API before shipping.
            val rawMap: Map<String, Int> = invResponse.body()
            val items = rawMap.entries.associate { (idStr, qty) ->
                val id = idStr.toIntOrNull()
                    ?: return@associate idStr.hashCode() to InventoryItem(idStr.hashCode(), idStr, qty, ItemType.OTHER)
                id to InventoryItem(id, "Item #$id", qty, ItemType.OTHER)
            }
            _state.value = _state.value.copy(items = items, isStale = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isStale = true)
        }
    }

    suspend fun useItem(item: InventoryItem): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/inv_use.php",
            formParameters = parameters {
                append("which", "3")
                append("whichitem", item.itemId.toString())
            }
        )
        eventBus.emit(GameEvent.ItemConsumed(item.itemId, 1))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun equipItem(item: InventoryItem, slot: String): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/inv_equip.php",
            formParameters = parameters {
                append("which", "2")
                append("action", "equip")
                append("whichitem", item.itemId.toString())
                append("slot", slot)
                append("ajax", "1")
            }
        )
        eventBus.emit(GameEvent.ItemEquipped(item, slot))
        syncCharacterEquipment()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun syncCharacterEquipment() {
        val req = characterRequest ?: return
        val char = character ?: return
        req.fetchCharacterState().onSuccess { char.updateFromApiResponse(it) }
    }

    open suspend fun unequipSlot(slot: String): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/inv_equip.php",
            formParameters = parameters {
                append("action", "unequip")
                append("type", slot)
            }
        )
        fetchInventory()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun discardItem(item: InventoryItem, quantity: Int): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/multiuse.php",
            formParameters = parameters {
                append("action", "trash")
                append("whichitem", item.itemId.toString())
                append("quantity", quantity.toString())
            }
        )
        eventBus.emit(GameEvent.ItemDiscarded(item.itemId, quantity))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun craft(mode: CraftMode, item1Id: Int, item2Id: Int): Result<InventoryItem> {
        val request = net.sourceforge.kolmafia.request.CraftRequest(client)
        val created = request.craft(mode, 1, item1Id, item2Id)
        if (created <= 0) return Result.failure(Exception("Craft failed"))
        fetchInventory()
        val placeholder = InventoryItem(item1Id, "Crafted item", created, ItemType.OTHER)
        eventBus.emit(GameEvent.ItemCrafted(placeholder))
        return Result.success(placeholder)
    }

    suspend fun mallSearch(query: String): Result<List<MallListing>> {
        // Mall search response is HTML — HTML parser not yet implemented.
        // Returns empty list until HTML parsing is added in a future task.
        return Result.success(emptyList())
    }

    suspend fun mallBuy(storeId: Int, itemId: Int, quantity: Int): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/mallstore.php",
            formParameters = parameters {
                append("whichstore", storeId.toString())
                append("buying", itemId.toString())
                append("quantity", quantity.toString())
            }
        )
        val body = response.bodyAsText()
        when {
            body.contains("That item is not available") -> Result.failure(MallError.SoldOut)
            body.contains("You can't afford") -> Result.failure(MallError.InsufficientMeat)
            else -> {
                fetchInventory()
                Result.success(Unit)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
