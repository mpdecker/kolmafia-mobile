package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.ZapRequest
import net.sourceforge.kolmafia.session.WandDiscovery

class GameRuntimeLibraryAshP224Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun zap_returnsAcquiredItemName() {
        registerItem(WandDiscovery.PINE_WAND, "pine wand")
        registerItem(SOURCE_ITEM, "zap source")
        registerItem(TARGET_ITEM, "zap target")
        val inventory = fakeInventoryManager(
            mapOf(
                WandDiscovery.PINE_WAND to InventoryItem(WandDiscovery.PINE_WAND, "pine wand", 1, ItemType.OTHER),
                SOURCE_ITEM to InventoryItem(SOURCE_ITEM, "zap source", 1, ItemType.OTHER),
            ),
        )
        val engine = MockEngine {
            respond(
                "You acquire an item: <b>zap target</b>",
                HttpStatusCode.OK,
            )
        }
        val zapRequest = ZapRequest(
            client = HttpClient(engine),
            inventoryManager = inventory,
            preferences = prefs(),
        )
        val db = GameDatabase()
        val lib = GameRuntimeLibrary(
            inventoryManager = inventory,
            zapRequest = zapRequest,
            gameDatabase = db,
            preferences = prefs(),
        )
        val out = outputLib(
            lib,
            """
                item result = zap(to_item("zap source"));
                print(to_string(result));
            """.trimIndent(),
        )
        assertEquals("zap target", out.trim())
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
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun fakeInventoryManager(items: Map<Int, InventoryItem>): InventoryManager =
        object : InventoryManager(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }), GameEventBus()) {
            private val flow = MutableStateFlow(InventoryState(items = items))
            override val state = flow.asStateFlow()
            override suspend fun fetchInventory() { /* no-op */ }
            override fun consumeItemLocally(itemId: Int, quantity: Int) {
                if (quantity <= 0) return
                val current = flow.value
                val item = current.items[itemId] ?: return
                val remaining = item.quantity - quantity
                val updated = current.items.toMutableMap()
                if (remaining <= 0) {
                    updated.remove(itemId)
                } else {
                    updated[itemId] = item.copy(quantity = remaining)
                }
                flow.value = current.copy(items = updated)
            }
        }

    companion object {
        private const val SOURCE_ITEM = 9301
        private const val TARGET_ITEM = 9302
    }
}
