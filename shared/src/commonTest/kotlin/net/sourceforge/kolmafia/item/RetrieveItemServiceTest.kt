package net.sourceforge.kolmafia.item

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.StorageRequest
import kotlin.test.*

// --- Shared test helpers ---

private const val ITEM_ID = 42
private const val ITEM_NAME = "test widget"

private fun testItemData() = ItemData(
    id = ITEM_ID, name = ITEM_NAME, descId = "", image = "",
    primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
    access = setOf('t', 'd'), autosellPrice = 10, plural = null
)

private fun dbWithNpc(npcStore: NpcStoreData? = NpcStoreData("generalstore", "General Store", "NPC")) =
    object : GameDatabase() {
        override fun item(id: Int) = if (id == ITEM_ID) testItemData() else null
        override fun item(name: String) = if (name == ITEM_NAME) testItemData() else null
        override fun npcStoreFor(itemName: String): NpcStoreData? =
            if (itemName == ITEM_NAME) npcStore else null
    }

private fun dbNoNpc() = dbWithNpc(npcStore = null)

private fun fakeInventory(qty: Int): InventoryManager {
    val items = if (qty > 0) mapOf(ITEM_ID to InventoryItem(ITEM_ID, ITEM_NAME, qty, ItemType.OTHER))
    else emptyMap()
    return object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
        private val _s = MutableStateFlow(InventoryState(items = items))
        override val state: StateFlow<InventoryState> = _s
    }
}

/** Inventory that gains [qtyOnFetch] items when [fetchInventory] is called (simulates withdraw). */
private fun inventoryGainingOnFetch(qtyOnFetch: Int, initialQty: Int = 0): InventoryManager {
    return object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
        private val _s = MutableStateFlow(
            InventoryState(items = if (initialQty > 0) mapOf(
                ITEM_ID to InventoryItem(ITEM_ID, ITEM_NAME, initialQty, ItemType.OTHER)
            ) else emptyMap())
        )
        override val state: StateFlow<InventoryState> = _s
        override suspend fun fetchInventory() {
            if (qtyOnFetch > 0) {
                _s.value = InventoryState(items = mapOf(
                    ITEM_ID to InventoryItem(ITEM_ID, ITEM_NAME, qtyOnFetch, ItemType.OTHER)
                ))
            }
        }
    }
}

private fun closetSucceeds() = object : ClosetRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun takeOut(itemId: Int, quantity: Int) = Result.success("ok")
}

private fun closetFails() = object : ClosetRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun takeOut(itemId: Int, quantity: Int) = Result.failure<String>(Exception("empty"))
}

private fun storageSucceeds() = StorageRequest(
    HttpClient(MockEngine { respond("<ok>", HttpStatusCode.OK) })
)

private fun storageFails() = StorageRequest(
    HttpClient(MockEngine { throw Exception("empty") })
)

private fun npcSucceeds(qty: Int) = object : NpcBuyRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun buy(storeKey: String, itemId: Int, quantity: Int) = Result.success(qty)
}

private fun npcFails() = object : NpcBuyRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun buy(storeKey: String, itemId: Int, quantity: Int) = Result.success(0)
}

private fun displaySucceeds() = object : net.sourceforge.kolmafia.request.DisplayCaseRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun takeOut(itemId: Int, quantity: Int) = Result.success("ok")
}

private fun stashSucceeds() = object : net.sourceforge.kolmafia.request.ClanStashRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun takeOut(itemId: Int, quantity: Int) = Result.success("ok")
}

private fun displayFails() = object : net.sourceforge.kolmafia.request.DisplayCaseRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun takeOut(itemId: Int, quantity: Int) = Result.failure<String>(Exception("empty"))
}

private fun stashFails() = object : net.sourceforge.kolmafia.request.ClanStashRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun takeOut(itemId: Int, quantity: Int) = Result.failure<String>(Exception("empty"))
}

private fun hermitSucceeds(): net.sourceforge.kolmafia.request.HermitRequest =
    object : net.sourceforge.kolmafia.request.HermitRequest(HttpClient(MockEngine { respond("") })) {
        override suspend fun trade(itemId: Int, quantity: Int) = Result.success("ok")
    }

private fun hermitFails(): net.sourceforge.kolmafia.request.HermitRequest =
    object : net.sourceforge.kolmafia.request.HermitRequest(HttpClient(MockEngine { respond("") })) {
        override suspend fun trade(itemId: Int, quantity: Int) =
            Result.failure<String>(Exception("not tradable"))
    }

private fun mallSucceeds(qty: Int): MallManager {
    val dummyClient = HttpClient(MockEngine { respond("") })
    return object : MallManager(MallSearchRequest(dummyClient), MallPurchaseRequest(dummyClient), null) {
        override suspend fun buy(itemId: Int, count: Int, maxPrice: Int) = qty
    }
}

// --- Tests ---

class RetrieveItemServiceTest {

    @Test
    fun retrieve_itemAlreadyInInventory_returnsQtyWithoutFetching() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 5),
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(3, service.retrieve(ITEM_ID, 3))
    }

    @Test
    fun retrieve_takesFromCloset_whenInventoryShort() = runTest {
        val service = RetrieveItemService(
            inventoryManager = inventoryGainingOnFetch(qtyOnFetch = 2),
            closetRequest = closetSucceeds(),
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(2, service.retrieve(ITEM_ID, 2))
    }

    @Test
    fun retrieve_pullsFromStorage_whenClosetFails() = runTest {
        val service = RetrieveItemService(
            inventoryManager = inventoryGainingOnFetch(qtyOnFetch = 2),
            closetRequest = closetFails(),
            storageRequest = storageSucceeds(),
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(2, service.retrieve(ITEM_ID, 2))
    }

    @Test
    fun retrieve_buysFromNpc_whenStorageFails() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = closetFails(),
            storageRequest = storageFails(),
            npcBuyRequest = npcSucceeds(2),
            mallManager = null,
            gameDatabase = dbWithNpc()
        )
        assertEquals(2, service.retrieve(ITEM_ID, 2))
    }

    @Test
    fun retrieve_skipsNpc_whenNoNpcStoreForItem() = runTest {
        var npcCalled = false
        val fakeNpc = object : NpcBuyRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun buy(storeKey: String, itemId: Int, quantity: Int): Result<Int> {
                npcCalled = true
                return Result.success(quantity)
            }
        }
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = closetFails(),
            storageRequest = storageFails(),
            npcBuyRequest = fakeNpc,
            mallManager = mallSucceeds(1),
            gameDatabase = dbNoNpc()   // no NPC store → NpcBuyRequest should NOT be called
        )
        service.retrieve(ITEM_ID, 1)
        assertFalse(npcCalled, "NpcBuyRequest.buy() must not be called when no NPC store sells the item")
    }

    @Test
    fun retrieve_buysFromMall_whenNpcFails() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = closetFails(),
            storageRequest = storageFails(),
            npcBuyRequest = npcFails(),
            mallManager = mallSucceeds(3),
            gameDatabase = dbWithNpc()
        )
        assertEquals(3, service.retrieve(ITEM_ID, 3))
    }

    @Test
    fun retrieve_tradesHermit_whenEarlierSourcesFail() = runTest {
        val service = RetrieveItemService(
            inventoryManager = inventoryGainingOnFetch(qtyOnFetch = 2),
            closetRequest = closetFails(),
            storageRequest = storageFails(),
            npcBuyRequest = npcFails(),
            mallManager = null,
            gameDatabase = dbNoNpc(),
            hermitRequest = hermitSucceeds(),
        )
        assertEquals(2, service.retrieve(ITEM_ID, 2))
    }

    @Test
    fun retrieve_skipsHermitOnFailure() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = closetFails(),
            storageRequest = storageFails(),
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc(),
            hermitRequest = hermitFails(),
        )
        assertEquals(0, service.retrieve(ITEM_ID, 1))
    }

    @Test
    fun retrieve_unknownItemId_returnsZero() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        // itemId 999 not in database → cannot resolve item name → 0
        assertEquals(0, service.retrieve(999, 2))
    }

    @Test
    fun retrieve_allSourcesNull_returnsZero() = runTest {
        val service = RetrieveItemService(
            inventoryManager = null,
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(0, service.retrieve(ITEM_ID, 1))
    }

    @Test
    fun retrieve_takesFromDisplay_whenStorageFails() = runTest {
        val service = RetrieveItemService(
            inventoryManager = inventoryGainingOnFetch(qtyOnFetch = 2),
            closetRequest = closetFails(),
            storageRequest = storageFails(),
            displayCaseRequest = displaySucceeds(),
            clanStashRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(2, service.retrieve(ITEM_ID, 2))
    }

    @Test
    fun retrieve_takesFromStash_whenDisplayFails() = runTest {
        val service = RetrieveItemService(
            inventoryManager = inventoryGainingOnFetch(qtyOnFetch = 2),
            closetRequest = closetFails(),
            storageRequest = storageFails(),
            displayCaseRequest = displayFails(),
            clanStashRequest = stashSucceeds(),
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(2, service.retrieve(ITEM_ID, 2))
    }

    @Test
    fun retrieve_closetHttpSuccessButPartialMove_returnsActualQty() = runTest {
        var fetchCount = 0
        val inv = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            private val _s = MutableStateFlow(InventoryState(items = emptyMap()))
            override val state: StateFlow<InventoryState> = _s
            override suspend fun fetchInventory() {
                fetchCount++
                if (fetchCount == 1) {
                    _s.value = InventoryState(items = mapOf(
                        ITEM_ID to InventoryItem(ITEM_ID, ITEM_NAME, 1, ItemType.OTHER)
                    ))
                }
            }
        }
        val service = RetrieveItemService(
            inventoryManager = inv,
            closetRequest = closetSucceeds(),
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(1, service.retrieve(ITEM_ID, 3))
    }

    @Test
    fun retrieve_craftsSuse_whenInventoryGainsOnUse() = runTest {
        ConcoctionDatabase.resetForTest()
        ConcoctionDatabase.injectForTest(
            net.sourceforge.kolmafia.data.ConcoctionData(
                result = ITEM_NAME,
                resultQuantity = 1,
                methods = setOf("SUSE"),
                ingredients = listOf(net.sourceforge.kolmafia.data.ConcoctionIngredient("source item", 1)),
            )
        )
        val sourceId = 99
        val db = object : GameDatabase() {
            override fun item(id: Int) = when (id) {
                ITEM_ID -> testItemData()
                sourceId -> ItemData(
                    id = sourceId, name = "source item", descId = "", image = "",
                    primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
                    access = setOf('t'), autosellPrice = 0, plural = null
                )
                else -> null
            }
            override fun item(name: String) = when (name) {
                ITEM_NAME -> testItemData()
                "source item" -> item(sourceId)
                else -> null
            }
        }
        var uses = 0
        val use = object : net.sourceforge.kolmafia.request.UseItemRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun use(itemId: Int, quantity: Int): Result<String> {
                uses++
                return Result.success("")
            }
        }
        val inv = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            private val _s = MutableStateFlow(
                InventoryState(items = mapOf(
                    sourceId to InventoryItem(sourceId, "source item", 2, ItemType.OTHER),
                ))
            )
            override val state: StateFlow<InventoryState> = _s
            override suspend fun fetchInventory() {
                _s.value = InventoryState(items = buildMap {
                    if (uses > 0) put(ITEM_ID, InventoryItem(ITEM_ID, ITEM_NAME, uses, ItemType.OTHER))
                    val sourceLeft = (2 - uses).coerceAtLeast(0)
                    if (sourceLeft > 0) put(sourceId, InventoryItem(sourceId, "source item", sourceLeft, ItemType.OTHER))
                })
            }
        }
        val service = RetrieveItemService(
            inventoryManager = inv,
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            useItemRequest = use,
            gameDatabase = db,
        )
        try {
            assertEquals(2, service.retrieve(ITEM_ID, 2))
            assertEquals(2, uses)
        } finally {
            ConcoctionDatabase.resetForTest()
        }
    }

    @Test
    fun retrieve_craftsAtStation_whenIngredientsAvailable() = runTest {
        ConcoctionDatabase.resetForTest()
        ConcoctionDatabase.injectForTest(
            net.sourceforge.kolmafia.data.ConcoctionData(
                result = ITEM_NAME,
                resultQuantity = 1,
                methods = setOf("COOK"),
                ingredients = listOf(
                    net.sourceforge.kolmafia.data.ConcoctionIngredient("ingredient a", 1),
                    net.sourceforge.kolmafia.data.ConcoctionIngredient("ingredient b", 1),
                ),
            )
        )
        val ingA = 50
        val ingB = 51
        val db = object : GameDatabase() {
            override fun item(id: Int) = when (id) {
                ITEM_ID -> testItemData()
                ingA -> ItemData(ingA, "ingredient a", "", "", ItemPrimaryUse.NONE, emptySet(), setOf('t'), 0, null)
                ingB -> ItemData(ingB, "ingredient b", "", "", ItemPrimaryUse.NONE, emptySet(), setOf('t'), 0, null)
                else -> null
            }
            override fun item(name: String) = when (name) {
                ITEM_NAME -> testItemData()
                "ingredient a" -> item(ingA)
                "ingredient b" -> item(ingB)
                else -> null
            }
        }
        val inv = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            private val _s = MutableStateFlow(
                InventoryState(items = mapOf(
                    ingA to InventoryItem(ingA, "ingredient a", 2, ItemType.OTHER),
                    ingB to InventoryItem(ingB, "ingredient b", 2, ItemType.OTHER),
                ))
            )
            override val state: StateFlow<InventoryState> = _s
            override suspend fun fetchInventory() {
                _s.value = InventoryState(items = mapOf(
                    ITEM_ID to InventoryItem(ITEM_ID, ITEM_NAME, 2, ItemType.OTHER),
                ))
            }
        }
        val craft = object : net.sourceforge.kolmafia.request.CraftRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun craft(mode: String, quantity: Int, itemId1: Int, itemId2: Int): Int = quantity
        }
        val service = RetrieveItemService(
            inventoryManager = inv,
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            craftRequest = craft,
            gameDatabase = db,
        )
        try {
            assertEquals(2, service.retrieve(ITEM_ID, 2))
        } finally {
            ConcoctionDatabase.resetForTest()
        }
    }

    @Test
    fun retrieve_craftFailure_returnsZero() = runTest {
        ConcoctionDatabase.resetForTest()
        ConcoctionDatabase.injectForTest(
            net.sourceforge.kolmafia.data.ConcoctionData(
                result = ITEM_NAME,
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    net.sourceforge.kolmafia.data.ConcoctionIngredient("ingredient a", 1),
                    net.sourceforge.kolmafia.data.ConcoctionIngredient("ingredient b", 1),
                ),
            )
        )
        val ingA = 50
        val ingB = 51
        val db = object : GameDatabase() {
            override fun item(id: Int) = when (id) {
                ITEM_ID -> testItemData()
                ingA -> ItemData(ingA, "ingredient a", "", "", ItemPrimaryUse.NONE, emptySet(), setOf('t'), 0, null)
                ingB -> ItemData(ingB, "ingredient b", "", "", ItemPrimaryUse.NONE, emptySet(), setOf('t'), 0, null)
                else -> null
            }
            override fun item(name: String) = when (name) {
                ITEM_NAME -> testItemData()
                "ingredient a" -> item(ingA)
                "ingredient b" -> item(ingB)
                else -> null
            }
        }
        val inv = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            private val _s = MutableStateFlow(
                InventoryState(items = mapOf(
                    ingA to InventoryItem(ingA, "ingredient a", 1, ItemType.OTHER),
                    ingB to InventoryItem(ingB, "ingredient b", 1, ItemType.OTHER),
                ))
            )
            override val state: StateFlow<InventoryState> = _s
        }
        val craft = object : net.sourceforge.kolmafia.request.CraftRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun craft(mode: String, quantity: Int, itemId1: Int, itemId2: Int): Int = 0
        }
        val service = RetrieveItemService(
            inventoryManager = inv,
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            craftRequest = craft,
            gameDatabase = db,
        )
        try {
            assertEquals(0, service.retrieve(ITEM_ID, 1))
        } finally {
            ConcoctionDatabase.resetForTest()
        }
    }
}
