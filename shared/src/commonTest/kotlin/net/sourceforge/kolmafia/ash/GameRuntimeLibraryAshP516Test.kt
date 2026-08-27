package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest

class GameRuntimeLibraryAshP516Test {

    data class BuyCall(val itemId: Int, val count: Int, val maxPrice: Int)

    private class RecordingMallManager : MallManager(
        MallSearchRequest(HttpClient(MockEngine { respond("") })),
        MallPurchaseRequest(HttpClient(MockEngine { respond("") })),
        null,
    ) {
        val calls = mutableListOf<BuyCall>()
        override suspend fun buy(itemId: Int, count: Int, maxPrice: Int): Int {
            calls += BuyCall(itemId, count, maxPrice)
            return count
        }
    }

    @BeforeTest
    fun loadItems() = runBlocking { ItemDatabase.load() }

    @Test
    fun revision_phase516() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun buy_qtyAndLimit_recordsMallPurchase() {
        val mall = RecordingMallManager()
        val out = outputLib(
            GameRuntimeLibrary(mallManager = mall),
            """cli_execute("buy 2 seal tooth @ 50");""",
        )
        assertEquals(listOf(BuyCall(2, 2, 50)), mall.calls)
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun mallbuy_defaultQty_recordsBaconstone() {
        val mall = RecordingMallManager()
        val out = outputLib(
            GameRuntimeLibrary(mallManager = mall),
            """cli_execute("mallbuy baconstone");""",
        )
        assertEquals(listOf(BuyCall(705, 1, Int.MAX_VALUE)), mall.calls)
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun buy_unknownItem_isSilent() {
        val mall = RecordingMallManager()
        val out = outputLib(
            GameRuntimeLibrary(mallManager = mall),
            """cli_execute("buy zzznosuchitem999");""",
        )
        assertEquals(emptyList(), mall.calls)
        assertTrue(out.isEmpty() || !out.contains("[cli]"))
        assertEquals("", out)
    }
}
