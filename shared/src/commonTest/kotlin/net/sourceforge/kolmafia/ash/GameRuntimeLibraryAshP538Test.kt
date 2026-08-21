package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ConcoctionCreatableEntry
import net.sourceforge.kolmafia.data.ConcoctionCreatableRegistry
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService

class GameRuntimeLibraryAshP538Test {

    data class RetrieveCall(val itemId: Int, val qty: Int)

    private class RecordingRetrieve : RetrieveItemService(
        inventoryManager = InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()),
        closetRequest = null,
        storageRequest = null,
        npcBuyRequest = null,
        mallManager = null,
        gameDatabase = net.sourceforge.kolmafia.data.GameDatabase(),
    ) {
        val calls = mutableListOf<RetrieveCall>()

        override suspend fun retrieve(itemId: Int, qty: Int): Int {
            calls += RetrieveCall(itemId, qty)
            return qty
        }
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
        ConcoctionCreatableRegistry.resetForTest()
    }

    @Test
    fun revision_phase538() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun create_qtyOptional_commaList() {
        val retrieve = RecordingRetrieve()
        outputLib(
            GameRuntimeLibrary(
                gameDatabase = net.sourceforge.kolmafia.data.GameDatabase(),
                retrieveItemService = retrieve,
            ),
            """cli_execute("create seal tooth, 3 helmet");""",
        )
        assertEquals(
            listOf(RetrieveCall(2, 1), RetrieveCall(3, 3)),
            retrieve.calls,
        )
    }

    @Test
    fun create_bare_printsCreatablesOrEmpty() {
        ConcoctionCreatableRegistry.seedForTest(
            ConcoctionCreatableEntry(
                resultName = "test smoothie",
                itemId = 9001,
                creatable = 2,
                pullable = 0,
                methods = setOf("MIX"),
            ),
        )
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("create");""")
        assertTrue(out.contains("test smoothie") || out.contains("nothing creatable"))
        assertTrue(out.contains("test smoothie (2)"))
    }

    @Test
    fun create_bare_empty_printsNothingCreatable() {
        ConcoctionCreatableRegistry.resetForTest()
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("create");""")
        assertTrue(out.contains("nothing creatable"))
    }
}
