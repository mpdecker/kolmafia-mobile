package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UntinkerRequest

class GameRuntimeLibraryAshP226Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        UntinkerRequest.resetForTest()
    }

    @Test
    fun revision_isphase222() {
        assertEquals("phase440", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun untinker_cliUntinkersInventoryItem() {
        registerItem(COMBINE_ITEM, "combining item")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "combining item",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("meat paste", 1)),
            ),
        )

        val inventory = fakeInventoryManager(
            mapOf(COMBINE_ITEM to InventoryItem(COMBINE_ITEM, "combining item", 1, ItemType.OTHER)),
        )
        val untinkerRequest = UntinkerRequest(
            client = HttpClient(
                MockEngine {
                    respond("You acquire an item: <b>meat paste</b>", HttpStatusCode.OK)
                },
            ),
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
        )
        val lib = GameRuntimeLibrary(
            inventoryManager = inventory,
            untinkerRequest = untinkerRequest,
            gameDatabase = GameDatabase(),
            preferences = prefs(),
        )

        runLib(lib, """cli_execute("untinker combining item");""")

        assertEquals(null, inventory.state.value.items[COMBINE_ITEM])
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

    private fun prefs(): Preferences = Preferences(MapSettings())

    companion object {
        private const val COMBINE_ITEM = 9601
    }
}
