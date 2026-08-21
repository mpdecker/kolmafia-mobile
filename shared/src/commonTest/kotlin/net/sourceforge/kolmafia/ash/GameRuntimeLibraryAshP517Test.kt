package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.AutosellRequest

class GameRuntimeLibraryAshP517Test {

    private class RecordingAutosell : AutosellRequest(HttpClient(MockEngine { respond("") })) {
        val calls = mutableListOf<Pair<Int, Int>>()
        override suspend fun autosell(itemId: Int, quantity: Int): Result<String> {
            calls += itemId to quantity
            return Result.success("ok")
        }
    }

    private class FakeInv(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state: StateFlow<InventoryState> = flow
    }

    @BeforeTest
    fun loadItems() = runBlocking { ItemDatabase.load() }

    @Test
    fun revision_phase517() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun autosell_qtyItem_recordsCount() {
        val sell = RecordingAutosell()
        val out = outputLib(
            GameRuntimeLibrary(autosellRequest = sell),
            """cli_execute("autosell 3 seal tooth");""",
        )
        assertEquals(listOf(2 to 3), sell.calls)
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun sell_commaList_usesInventoryCounts() {
        val sell = RecordingAutosell()
        val inv = FakeInv(
            mapOf(
                2 to InventoryItem(2, "seal tooth", 4, ItemType.OTHER),
                705 to InventoryItem(705, "baconstone", 2, ItemType.OTHER),
            ),
        )
        val out = outputLib(
            GameRuntimeLibrary(autosellRequest = sell, inventoryManager = inv),
            """cli_execute("sell seal tooth, baconstone");""",
        )
        assertEquals(listOf(2 to 4, 705 to 2), sell.calls)
        assertFalse(out.contains("[cli]"))
    }
}
