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

class InventoryManager(
    private val client: HttpClient,
    private val eventBus: GameEventBus
) {
    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

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

    suspend fun fetchInventory() {
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

    suspend fun equipItem(item: InventoryItem, slot: String): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/inv_equip.php",
            formParameters = parameters {
                append("which", "2")
                append("whichitem", item.itemId.toString())
                append("slot", slot)
            }
        )
        eventBus.emit(GameEvent.ItemEquipped(item, slot))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun unequipSlot(slot: String): Result<Unit> = try {
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

    suspend fun craft(mode: CraftMode, item1Id: Int, item2Id: Int): Result<InventoryItem> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/craft.php",
            formParameters = parameters {
                append("action", mode.apiAction)
                if (mode == CraftMode.COMBINE) append("mode", "combine")
                append("item1", item1Id.toString())
                append("item2", item2Id.toString())
            }
        )
        val placeholder = InventoryItem(-1, "Crafted item", 1, ItemType.OTHER)
        eventBus.emit(GameEvent.ItemCrafted(placeholder))
        fetchInventory()
        Result.success(placeholder)
    } catch (e: Exception) {
        Result.failure(e)
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
