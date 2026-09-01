package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.DisplayCaseRequest

class GameRuntimeLibraryAshP534Test {

    data class DisplayCall(val op: String, val itemId: Int, val qty: Int)

    private class RecordingDisplay : DisplayCaseRequest(HttpClient(MockEngine { respond("ok") })) {
        val calls = mutableListOf<DisplayCall>()
        private var contents: Map<Int, Int> = emptyMap()

        fun seedContents(map: Map<Int, Int>) {
            contents = map
        }

        override suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
            calls += DisplayCall("put", itemId, quantity)
            return Result.success("ok")
        }

        override suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
            calls += DisplayCall("take", itemId, quantity)
            return Result.success("ok")
        }

        override suspend fun fetchContents(): Map<Int, Int> = contents
    }

    private fun invWith(vararg items: Pair<Int, Int>): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok") }),
            GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(
                    items = items.associate { (id, qty) ->
                        id to InventoryItem(id, "item$id", qty, ItemType.OTHER)
                    },
                ),
            ).asStateFlow()
        }

    @BeforeTest
    fun setUp() {
        ItemDatabase.registerForTest(
            ItemData(2, "seal tooth", "d2", "t.gif", ItemPrimaryUse.FOOD, emptySet(), setOf('t'), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(3, "helmet", "d3", "h.gif", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
        )
    }

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase534() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun display_put_commaList_qtyOptional() {
        val display = RecordingDisplay()
        val out = outputLib(
            GameRuntimeLibrary(
                displayCaseRequest = display,
                inventoryManager = invWith(2 to 3, 3 to 1),
            ),
            """cli_execute("display put 2 seal tooth, helmet");""",
        )
        assertEquals(
            listOf(DisplayCall("put", 2, 2), DisplayCall("put", 3, 1)),
            display.calls,
        )
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun display_take_commaList_usesDisplayCount() {
        val display = RecordingDisplay().also { it.seedContents(mapOf(2 to 4, 3 to 1)) }
        outputLib(
            GameRuntimeLibrary(displayCaseRequest = display),
            """cli_execute("display take seal tooth, 2 helmet");""",
        )
        assertEquals(
            listOf(DisplayCall("take", 2, 4), DisplayCall("take", 3, 2)),
            display.calls,
        )
    }
}
