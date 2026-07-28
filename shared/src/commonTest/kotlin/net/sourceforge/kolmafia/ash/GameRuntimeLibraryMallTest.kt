package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.StorageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlin.test.*

private const val TEST_ITEM = "test widget"
private const val TEST_ITEM_ID = 42

private fun stubDb(): GameDatabase = object : GameDatabase() {
    override fun item(id: Int) = if (id == TEST_ITEM_ID) ItemData(
        id = TEST_ITEM_ID, name = TEST_ITEM, descId = "", image = "",
        primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
        access = setOf('t', 'd'), autosellPrice = 10, plural = null
    ) else null
    override fun item(name: String) = if (name == TEST_ITEM) item(TEST_ITEM_ID) else null
}

private fun mallThatBuys(qty: Int): MallManager {
    val dummyClient = HttpClient(MockEngine { respond("") })
    return object : MallManager(MallSearchRequest(dummyClient), MallPurchaseRequest(dummyClient), null) {
        override suspend fun buy(itemId: Int, count: Int, maxPrice: Int) = qty
        override suspend fun cheapestPrice(itemName: String) = if (itemName == TEST_ITEM) 500L else -1L
    }
}

// Returns a service that always reports full success (returns whatever qty was requested).
private fun retrieveAlwaysSucceeds(): RetrieveItemService =
    object : RetrieveItemService(null, null, null, null, null, null, null, null, null, null, null) {
        override suspend fun retrieve(itemId: Int, qty: Int) = qty
    }

private class TestInventoryManager(
    items: Map<Int, InventoryItem>,
) : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
    private val flow = MutableStateFlow(InventoryState(items = items))
    override val state = flow.asStateFlow()
}

private class FakeStorageRequest(
    private val contents: Map<Int, Int>,
) : StorageRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun fetchRawContents(): Map<Int, Int> = contents
}

class GameRuntimeLibraryMallTest {

    @Test
    fun buy_callsMallManagerAndReturnsPurchasedCount() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            mallManager = mallThatBuys(2)
        )
        assertEquals("2", outputLib(lib, """print(to_string(buy(2, to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun buy_withMaxPrice_passesCapToMallManager() {
        val db = stubDb()
        var capturedMax = Int.MAX_VALUE
        val dummyClient = HttpClient(MockEngine { respond("") })
        val mall = object : MallManager(MallSearchRequest(dummyClient), MallPurchaseRequest(dummyClient), null) {
            override suspend fun buy(itemId: Int, count: Int, maxPrice: Int): Int {
                capturedMax = maxPrice
                return count
            }
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, mallManager = mall)
        outputLib(lib, """buy(1, to_item("$TEST_ITEM"), 1000);""")
        assertEquals(1000, capturedMax)
    }

    @Test
    fun buy_unknownItem_returnsZero() {
        val lib = GameRuntimeLibrary(gameDatabase = stubDb(), mallManager = mallThatBuys(1))
        assertEquals("0", outputLib(lib, """print(to_string(buy(1, to_item("unknown item xyz"))));"""))
    }

    @Test
    fun buy_nullMallManager_returnsZero() {
        val lib = GameRuntimeLibrary(gameDatabase = stubDb(), mallManager = null)
        assertEquals("0", outputLib(lib, """print(to_string(buy(1, to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun retrieveItem_successReturnsTrue() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            retrieveItemService = retrieveAlwaysSucceeds()
        )
        assertEquals("true", outputLib(lib, """print(to_string(retrieve_item(3, to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun retrieveItem_withRetrieveFlag_checkOnlyUsesAccessibleCount() {
        val inv = TestInventoryManager(
            mapOf(TEST_ITEM_ID to InventoryItem(TEST_ITEM_ID, TEST_ITEM, 5, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            retrieveItemService = retrieveAlwaysSucceeds(),
            inventoryManager = inv,
        )
        assertEquals("true", outputLib(lib, """print(to_string(retrieve_item(1, to_item("$TEST_ITEM"), false)));"""))
        assertEquals("false", outputLib(lib, """print(to_string(retrieve_item(10, to_item("$TEST_ITEM"), false)));"""))
    }

    @Test
    fun retrieveItem_nullService_returnsFalse() {
        val lib = GameRuntimeLibrary(gameDatabase = stubDb(), retrieveItemService = null)
        assertEquals("false", outputLib(lib, """print(to_string(retrieve_item(1, to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun mallPrice_returnsLowestListedPrice() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            mallManager = mallThatBuys(0)   // cheapestPrice returns 500L for TEST_ITEM
        )
        assertEquals("500", outputLib(lib, """print(to_string(mall_price(to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun mallPrice_nullMallManager_returnsMinusOne() {
        val lib = GameRuntimeLibrary(gameDatabase = stubDb(), mallManager = null)
        assertEquals("-1", outputLib(lib, """print(to_string(mall_price(to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun availableAmount_usesPhysicalAccessibleCount() {
        val inv = TestInventoryManager(
            mapOf(TEST_ITEM_ID to InventoryItem(TEST_ITEM_ID, TEST_ITEM, 2, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            inventoryManager = inv,
            storageRequest = FakeStorageRequest(mapOf(TEST_ITEM_ID to 5)),
        )
        assertEquals("7", outputLib(lib, """print(to_string(available_amount(to_item("$TEST_ITEM"))));"""))
    }
}
