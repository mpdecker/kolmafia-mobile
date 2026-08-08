package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences

class SewerCreateRequestTest {

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
    }

    private class TrackingClosetRequest(
        private val inventory: MutableMap<Int, Int>,
        private val putCalls: MutableList<Pair<Int, Int>>,
    ) : ClosetRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
        override suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
            putCalls += itemId to quantity
            val current = inventory[itemId] ?: 0
            if (current < quantity) return Result.failure(IllegalStateException("not enough"))
            val remaining = current - quantity
            if (remaining <= 0) inventory.remove(itemId) else inventory[itemId] = remaining
            return Result.success("ok")
        }
    }

    private class TrackingUseItemRequest(
        private val client: HttpClient,
        private val inventory: MutableMap<Int, Int>,
        private val goalItemId: Int,
        private val usedItems: MutableList<Int>,
    ) : UseItemRequest(client) {
        override suspend fun use(itemId: Int, quantity: Int): Result<String> {
            usedItems += itemId
            inventory[goalItemId] = (inventory[goalItemId] ?: 0) + 1
            return Result.success("You acquire an item: <b>seal-skull helmet</b>")
        }
    }

    @Test
    fun create_retrievesGumClosetsGoalAndUsesGum() = runTest {
        registerItem(23, "chewing gum on a string")
        registerItem(2283, "seal-skull helmet")
        val inventory = mutableMapOf(2283 to 1)
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val putCalls = mutableListOf<Pair<Int, Int>>()
        val usedItems = mutableListOf<Int>()
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = SewerCreateRequest(
            useItemRequest = TrackingUseItemRequest(client, inventory, 2283, usedItems),
            closetRequest = TrackingClosetRequest(inventory, putCalls),
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService { id, qty ->
                    retrieved += id to qty
                    qty
                },
                gameDatabase = null,
            ),
            gameDatabase = null,
            inventoryCountById = { id -> inventory[id] ?: 0 },
        )
        val concoction = ConcoctionData(
            result = "seal-skull helmet",
            resultQuantity = 1,
            methods = setOf("SEWER"),
            ingredients = listOf(ConcoctionIngredient("chewing gum on a string", 1)),
        )

        val result = request.create(concoction, 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(listOf(23 to 1, 23 to 1), retrieved)
        assertEquals(listOf(2283 to 1), putCalls)
        assertEquals(listOf(23), usedItems)
    }

    @Test
    fun create_useFailure_returnsFailure() = runTest {
        registerItem(23, "chewing gum on a string")
        registerItem(2283, "seal-skull helmet")
        val client = HttpClient(MockEngine { respond("nothing happened", HttpStatusCode.OK) })
        val request = SewerCreateRequest(
            useItemRequest = UseItemRequest(client),
            closetRequest = ClosetRequest(client),
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService(),
                gameDatabase = null,
            ),
            gameDatabase = null,
            inventoryCountById = { 0 },
        )
        val concoction = ConcoctionData(
            result = "seal-skull helmet",
            resultQuantity = 1,
            methods = setOf("SEWER"),
            ingredients = listOf(ConcoctionIngredient("chewing gum on a string", 1)),
        )

        val result = request.create(concoction, 1, state = null, preferences = null)

        assertTrue(result.isFailure)
    }

    @Test
    fun create_worthlessItemAlias_countsComponentIds() = runTest {
        registerItem(23, "chewing gum on a string")
        registerItem(43, "worthless trinket")
        val inventory = mutableMapOf<Int, Int>()
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = SewerCreateRequest(
            useItemRequest = object : UseItemRequest(client) {
                override suspend fun use(itemId: Int, quantity: Int): Result<String> {
                    inventory[43] = (inventory[43] ?: 0) + 1
                    return Result.success("You acquire an item: <b>worthless trinket</b>")
                }
            },
            closetRequest = TrackingClosetRequest(inventory, mutableListOf()),
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService(),
                gameDatabase = null,
            ),
            gameDatabase = null,
            inventoryCountById = { id -> inventory[id] ?: 0 },
        )
        val concoction = ConcoctionData(
            result = SewerCreateRequest.WORTHLESS_ITEM_NAME,
            resultQuantity = 1,
            methods = setOf("SEWER"),
            ingredients = listOf(ConcoctionIngredient("chewing gum on a string", 1)),
        )

        val result = request.create(concoction, 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(1, inventory[43])
    }

    @Test
    fun create_unknownGoalItem_returnsFailure() = runTest {
        registerItem(23, "chewing gum on a string")
        val client = HttpClient(MockEngine { respond("You acquire an item.", HttpStatusCode.OK) })
        val request = SewerCreateRequest(
            useItemRequest = UseItemRequest(client),
            closetRequest = ClosetRequest(client),
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService(),
                gameDatabase = null,
            ),
            gameDatabase = null,
        )
        val concoction = ConcoctionData(
            result = "unknown sewer item",
            resultQuantity = 1,
            methods = setOf("SEWER"),
            ingredients = listOf(ConcoctionIngredient("chewing gum on a string", 1)),
        )

        val result = request.create(concoction, 1, state = null, preferences = null)

        assertTrue(result.isFailure)
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
