package net.sourceforge.kolmafia.shop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoinmasterManagerTest {

    @Test
    fun findBuyRowForItem_locatesShoreRow() {
        val shore = CoinmasterRegistry.findByNickname("shore")!!
        val row = CoinmasterRegistry.findBuyRowForItem(146)
        assertEquals(shore, row?.first)
        assertEquals(176, row?.second?.rowId)
    }

    @Test
    fun buy_incrementsInventory() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val flow = MutableStateFlow(InventoryState())
        val inventory = object : InventoryManager(client, GameEventBus()) {
            override val state = flow
            override suspend fun fetchInventory() {
                flow.value = InventoryState(
                    items = mapOf(146 to InventoryItem(146, "dinghy plans", 1, ItemType.OTHER))
                )
            }
        }
        val manager = CoinmasterManager(
            coinmasterRequest = CoinmasterRequest(client),
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
            client = client,
        )
        val shore = CoinmasterRegistry.findByNickname("shore")!!
        assertEquals(1, manager.buy(shore, 146, 1))
    }

    @Test
    fun buysItem_returnsTrueForKnownRow() {
        val shore = CoinmasterRegistry.findByNickname("shore")!!
        val manager = CoinmasterManager(
            CoinmasterRequest(HttpClient(MockEngine { respond("") })),
            null,
            GameDatabase(),
            HttpClient(MockEngine { respond("") }),
        )
        assertTrue(manager.buysItem(shore, 146))
    }
}
