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
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP531Test {

    private class RecordingNpc : NpcBuyRequest(HttpClient(MockEngine { respond("ok") })) {
        var called = false
        override suspend fun buy(
            storeKey: String,
            itemId: Int,
            quantity: Int,
            prefs: Preferences?,
        ): Result<Int> {
            called = true
            return Result.success(quantity)
        }
    }

    private class RecordingMall : MallManager(
        MallSearchRequest(HttpClient(MockEngine { respond("") })),
        MallPurchaseRequest(HttpClient(MockEngine { respond("") })),
        null,
    ) {
        val buys = mutableListOf<Triple<Int, Int, Int>>()
        override suspend fun cheapestPrice(itemName: String): Long = 500
        override suspend fun buy(itemId: Int, count: Int, maxPrice: Int): Int {
            buys += Triple(itemId, count, maxPrice)
            return count
        }
    }

    @BeforeTest
    fun setUp() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 2,
                name = "seal tooth",
                descId = "d2",
                image = "tooth.gif",
                primaryUse = ItemPrimaryUse.FOOD,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        NpcStoreDatabase.loadFromText("General Store\tm\tseal tooth\t100\n")
    }

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase531() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun buy_qtyZero_errorsWithoutPurchase() {
        val mall = RecordingMall()
        val npc = RecordingNpc()
        val out = outputLib(
            GameRuntimeLibrary(mallManager = mall, npcBuyRequest = npc),
            """cli_execute("buy 0 seal tooth");""",
        )
        assertTrue(out.contains("Purchasing 0 of an item produces surprising results"))
        assertTrue(mall.buys.isEmpty())
        assertFalse(npc.called)
    }

    @Test
    fun buy_fromMall_forcesMallEvenWhenNpcCheaper() {
        val mall = RecordingMall()
        val npc = RecordingNpc()
        outputLib(
            GameRuntimeLibrary(mallManager = mall, npcBuyRequest = npc),
            """cli_execute("buy from mall seal tooth");""",
        )
        assertFalse(npc.called)
        assertEquals(listOf(Triple(2, 1, Int.MAX_VALUE)), mall.buys)
    }
}
