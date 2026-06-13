package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.equipment.OutfitManager
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
        val manager = object : OutfitManager(
            retrieveItemService = null,
            equipmentRequest = net.sourceforge.kolmafia.request.EquipmentRequest(
                HttpClient(MockEngine { respond("") })
            ),
            customOutfitRequest = net.sourceforge.kolmafia.request.CustomOutfitRequest(
                HttpClient(MockEngine { respond("") })
            ),
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            gameDatabase = stubDb(),
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override suspend fun accessibleCount(itemId: Int, itemName: String) = 5
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            retrieveItemService = retrieveAlwaysSucceeds(),
            outfitManager = manager,
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
    fun availableAmount_usesOutfitManagerAccessibleCount() {
        val manager = object : OutfitManager(
            retrieveItemService = null,
            equipmentRequest = net.sourceforge.kolmafia.request.EquipmentRequest(
                HttpClient(MockEngine { respond("") })
            ),
            customOutfitRequest = net.sourceforge.kolmafia.request.CustomOutfitRequest(
                HttpClient(MockEngine { respond("") })
            ),
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            gameDatabase = stubDb(),
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override suspend fun accessibleCount(itemId: Int, itemName: String) = 7
        }
        val lib = GameRuntimeLibrary(gameDatabase = stubDb(), outfitManager = manager)
        assertEquals("7", outputLib(lib, """print(to_string(available_amount(to_item("$TEST_ITEM"))));"""))
    }
}
