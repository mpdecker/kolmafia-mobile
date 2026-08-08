package net.sourceforge.kolmafia.request

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
import net.sourceforge.kolmafia.item.RetrieveItemService

class RollingPinCreateRequestTest {

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int,
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
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
