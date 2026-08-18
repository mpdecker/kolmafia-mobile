package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
import net.sourceforge.kolmafia.request.ClanStashRequest

class GameRuntimeLibraryAshP532Test {

    data class StashCall(val op: String, val itemId: Int, val qty: Int)

    private class RecordingStash : ClanStashRequest(HttpClient(MockEngine { respond("ok") })) {
        val calls = mutableListOf<StashCall>()
        override suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
            calls += StashCall("put", itemId, quantity)
            return Result.success("ok")
        }
        override suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
            calls += StashCall("take", itemId, quantity)
            return Result.success("ok")
        }
        override suspend fun fetchContents(): Map<Int, Int> = emptyMap()
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
    fun revision_phase532() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun stash_put_commaList_qtyOptional() {
        val stash = RecordingStash()
        val out = outputLib(
            GameRuntimeLibrary(
                clanStashRequest = stash,
                inventoryManager = invWith(2 to 3, 3 to 1),
            ),
            """cli_execute("stash put 2 seal tooth, helmet");""",
        )
        assertEquals(
            listOf(StashCall("put", 2, 2), StashCall("put", 3, 1)),
            stash.calls,
        )
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun stash_take_commaList() {
        val stash = RecordingStash()
        outputLib(
            GameRuntimeLibrary(clanStashRequest = stash),
            """cli_execute("stash take seal tooth, 5 helmet");""",
        )
        assertEquals(
            listOf(StashCall("take", 2, 1), StashCall("take", 3, 5)),
            stash.calls,
        )
    }

    @Test
    fun help_listsStash() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help stash");""")
        assertTrue(out.lines().any { it.trim() == "stash" })
    }
}
