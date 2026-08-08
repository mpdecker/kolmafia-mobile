package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.preferences.Preferences

class RollingPinCreateRequestTest {

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int,
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
    }

    private class RecordingNpcBuyRequest(
        client: HttpClient,
        private val onBuy: (storeKey: String, itemId: Int, quantity: Int) -> Unit = { _, _, _ -> },
    ) : NpcBuyRequest(client) {
        override suspend fun buy(
            storeKey: String,
            itemId: Int,
            quantity: Int,
            prefs: Preferences?,
        ): Result<Int> {
            onBuy(storeKey, itemId, quantity)
            return Result.success(quantity)
        }

        override suspend fun visitStore(
            storeKey: String,
            prefs: Preferences?,
            ascensionNumber: Int,
        ): Result<String> = Result.success("")
    }

    @Test
    fun create_flatDough_usesRollingPin() = runTest {
        registerItem(159, "wad of dough")
        registerItem(301, "flat dough")
        registerItem(873, "rolling pin")
        val retrieved = mutableListOf<Int>()
        val usedItemIds = mutableListOf<Int>()
        val client = HttpClient(MockEngine { request ->
            usedItemIds += request.url.parameters["whichitem"]?.toInt() ?: -1
            respond("You acquire an item: <b>flat dough</b>", HttpStatusCode.OK)
        })
        val retrieve = StubRetrieveItemService { id, qty ->
            retrieved += id
            qty
        }
        val request = RollingPinCreateRequest(
            useItemRequest = UseItemRequest(client),
            retrieveItemService = retrieve,
            gameDatabase = null,
            accessibleCountFn = accessibleCounts(
                159 to 1,
                873 to 1,
            ),
        )
        val concoction = ConcoctionData(
            result = "flat dough",
            resultQuantity = 1,
            methods = setOf("ROLL"),
            ingredients = listOf(ConcoctionIngredient("wad of dough", 1)),
        )

        val result = request.create(concoction, 1)

        assertTrue(result.isSuccess)
        assertEquals(listOf(159, 873), retrieved)
        assertEquals(listOf(873), usedItemIds)
    }

    @Test
    fun create_wadOfDough_usesUnrollingPin() = runTest {
        registerItem(159, "wad of dough")
        registerItem(301, "flat dough")
        registerItem(874, "unrolling pin")
        val usedItemIds = mutableListOf<Int>()
        val client = HttpClient(MockEngine { request ->
            usedItemIds += request.url.parameters["whichitem"]?.toInt() ?: -1
            respond("You acquire an item: <b>wad of dough</b>", HttpStatusCode.OK)
        })
        val retrieve = StubRetrieveItemService { _, qty -> qty }
        val request = RollingPinCreateRequest(
            useItemRequest = UseItemRequest(client),
            retrieveItemService = retrieve,
            gameDatabase = null,
            accessibleCountFn = accessibleCounts(
                301 to 1,
                874 to 1,
            ),
        )
        val concoction = ConcoctionData(
            result = "wad of dough",
            resultQuantity = 1,
            methods = setOf("ROLL"),
            ingredients = listOf(ConcoctionIngredient("flat dough", 1)),
        )

        val result = request.create(concoction, 1)

        assertTrue(result.isSuccess)
        assertEquals(listOf(874), usedItemIds)
    }

    @Test
    fun create_flatDough_buysWadsWhenInputMissing() = runTest {
        registerStoreData()
        registerItem(159, "wad of dough")
        registerItem(301, "flat dough")
        registerItem(873, "rolling pin")
        val buyCalls = mutableListOf<Pair<Int, Int>>()
        val usedItemIds = mutableListOf<Int>()
        val client = HttpClient(MockEngine { request ->
            usedItemIds += request.url.parameters["whichitem"]?.toInt() ?: -1
            respond("You acquire an item: <b>flat dough</b>", HttpStatusCode.OK)
        })
        val request = RollingPinCreateRequest(
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { id, qty ->
                if (id == 873) qty else 0
            },
            gameDatabase = null,
            npcBuyRequest = RecordingNpcBuyRequest(client) { _, itemId, quantity ->
                buyCalls += itemId to quantity
            },
            preferences = Preferences(MapSettings()),
            accessibleCountFn = accessibleCounts(873 to 1),
        )

        val result = request.create(
            ConcoctionData(
                result = "flat dough",
                resultQuantity = 1,
                methods = setOf("ROLL"),
                ingredients = listOf(ConcoctionIngredient("wad of dough", 1)),
            ),
            1,
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(159 to 1), buyCalls)
        assertEquals(listOf(873), usedItemIds)
    }

    @Test
    fun create_wadOfDough_buysWadsAndSkipsRoll() = runTest {
        registerStoreData()
        registerItem(159, "wad of dough")
        registerItem(301, "flat dough")
        registerItem(874, "unrolling pin")
        val buyCalls = mutableListOf<Pair<Int, Int>>()
        val usedItemIds = mutableListOf<Int>()
        val client = HttpClient(MockEngine { request ->
            usedItemIds += request.url.parameters["whichitem"]?.toInt() ?: -1
            respond("You acquire an item: <b>wad of dough</b>", HttpStatusCode.OK)
        })
        val request = RollingPinCreateRequest(
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { _, _ -> 0 },
            gameDatabase = null,
            npcBuyRequest = RecordingNpcBuyRequest(client) { _, itemId, quantity ->
                buyCalls += itemId to quantity
            },
            preferences = Preferences(MapSettings()),
        )

        val result = request.create(
            ConcoctionData(
                result = "wad of dough",
                resultQuantity = 2,
                methods = setOf("ROLL"),
                ingredients = listOf(ConcoctionIngredient("flat dough", 1)),
            ),
            2,
        )

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow())
        assertEquals(listOf(159 to 2), buyCalls)
        assertTrue(usedItemIds.isEmpty())
    }

    @Test
    fun create_flatDough_failsWhenNeededOver10WithoutPin() = runTest {
        registerItem(159, "wad of dough")
        registerItem(301, "flat dough")
        registerItem(873, "rolling pin")
        val client = HttpClient(MockEngine { respond("You acquire an item: <b>flat dough</b>", HttpStatusCode.OK) })
        val request = RollingPinCreateRequest(
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { _, _ -> 0 },
            gameDatabase = null,
            accessibleCountFn = accessibleCounts(159 to 11),
        )

        val result = request.create(
            ConcoctionData(
                result = "flat dough",
                resultQuantity = 1,
                methods = setOf("ROLL"),
                ingredients = listOf(ConcoctionIngredient("wad of dough", 1)),
            ),
            11,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("rolling pin") == true)
    }

    private fun accessibleCounts(vararg entries: Pair<Int, Int>): suspend (Int, String) -> Int = { id, _ ->
        entries.toMap()[id] ?: 0
    }

    private fun registerStoreData() {
        NpcStoreDatabase.loadFromText(
            """
            Degrassi Knoll Bakery and Hardware Store	gnoll	wad of dough	50	ROW600
            """.trimIndent(),
        )
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
    }
}
