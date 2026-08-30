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
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.mall.MallListing
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP528Test {

    data class NpcBuyCall(val storeKey: String, val itemId: Int, val quantity: Int)

    private class RecordingNpcBuy : NpcBuyRequest(HttpClient(MockEngine { respond("ok") })) {
        val calls = mutableListOf<NpcBuyCall>()
        override suspend fun buy(
            storeKey: String,
            itemId: Int,
            quantity: Int,
            prefs: Preferences?,
        ): Result<Int> {
            calls += NpcBuyCall(storeKey, itemId, quantity)
            return Result.success(quantity)
        }
    }

    private class RecordingMall(
        private val price: Long,
    ) : MallManager(
        MallSearchRequest(HttpClient(MockEngine { respond("") })),
        MallPurchaseRequest(HttpClient(MockEngine { respond("") })),
        null,
    ) {
        val buys = mutableListOf<Triple<Int, Int, Int>>()
        override suspend fun cheapestPrice(itemName: String): Long = price
        override suspend fun buy(itemId: Int, count: Int, maxPrice: Int): Int {
            buys += Triple(itemId, count, maxPrice)
            return count
        }
        override suspend fun searchListings(itemName: String, limit: Int): List<MallListing> =
            if (price < 0) emptyList() else listOf(MallListing(1, "", 0, price, 1))
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
    fun revision_phase528() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun buy_prefersNpcWhenCheaperThanMall() = runBlocking {
        val npc = RecordingNpcBuy()
        val mall = RecordingMall(price = 500)
        val out = outputLib(
            GameRuntimeLibrary(mallManager = mall, npcBuyRequest = npc),
            """cli_execute("buy seal tooth");""",
        )
        assertEquals(listOf(NpcBuyCall("m", 2, 1)), npc.calls)
        assertTrue(mall.buys.isEmpty())
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun buy_usesMallWhenCheaperThanNpc() {
        val npc = RecordingNpcBuy()
        val mall = RecordingMall(price = 50)
        outputLib(
            GameRuntimeLibrary(mallManager = mall, npcBuyRequest = npc),
            """cli_execute("buy seal tooth");""",
        )
        assertTrue(npc.calls.isEmpty())
        assertEquals(listOf(Triple(2, 1, Int.MAX_VALUE)), mall.buys)
    }
}
