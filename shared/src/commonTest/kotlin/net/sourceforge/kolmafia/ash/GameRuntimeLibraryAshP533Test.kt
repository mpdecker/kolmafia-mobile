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
import net.sourceforge.kolmafia.request.ClosetRequest

class GameRuntimeLibraryAshP533Test {

    data class ClosetCall(val op: String, val itemId: Int, val qty: Int)

    private class RecordingCloset : ClosetRequest(HttpClient(MockEngine { respond("ok") })) {
        val calls = mutableListOf<ClosetCall>()
        private var contents: Map<Int, Int> = emptyMap()

        fun seedContents(map: Map<Int, Int>) {
            contents = map
        }

        override suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
            calls += ClosetCall("put", itemId, quantity)
            return Result.success("ok")
        }

        override suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
            calls += ClosetCall("take", itemId, quantity)
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
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun closet_put_commaList_qtyOptional() {
        val closet = RecordingCloset()
        val out = outputLib(
            GameRuntimeLibrary(
                closetRequest = closet,
                inventoryManager = invWith(2 to 3, 3 to 1),
            ),
            """cli_execute("closet put 2 seal tooth, helmet");""",
        )
        assertEquals(
            listOf(ClosetCall("put", 2, 2), ClosetCall("put", 3, 1)),
            closet.calls,
        )
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun closet_take_commaList_usesClosetCount() {
        val closet = RecordingCloset().also { it.seedContents(mapOf(2 to 4, 3 to 1)) }
        outputLib(
            GameRuntimeLibrary(closetRequest = closet),
            """cli_execute("closet take seal tooth, 2 helmet");""",
        )
        assertEquals(
            listOf(ClosetCall("take", 2, 4), ClosetCall("take", 3, 2)),
            closet.calls,
        )
    }
}
