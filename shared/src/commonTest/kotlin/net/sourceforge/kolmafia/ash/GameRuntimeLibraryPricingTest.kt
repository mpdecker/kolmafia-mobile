package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond

/** Minimal GameDatabase stub that returns controlled items and NPC prices without loading from disk. */
private class StubPricingDatabase(
    private val fakeItemName: String? = null,
    private val fakeAutosellPrice: Int = 0,
    private val fakeNpcPrice: Int = 0,
) : GameDatabase() {

    private val fakeItem: ItemData? = fakeItemName?.let {
        ItemData(
            id = 9_000_042,
            name = it,
            descId = "desc",
            image = "item.gif",
            primaryUse = ItemPrimaryUse.NONE,
            secondaryUses = emptySet(),
            access = setOf('t', 'd'),
            autosellPrice = fakeAutosellPrice,
            plural = null
        )
    }

    override fun item(name: String): ItemData? =
        if (name.equals(fakeItemName, ignoreCase = true)) fakeItem else null

    override fun item(id: Int): ItemData? =
        if (fakeItem?.id == id) fakeItem else null

    override fun npcPrice(itemName: String): Int =
        if (itemName.equals(fakeItemName, ignoreCase = true)) fakeNpcPrice else 0
}

class GameRuntimeLibraryPricingTest {

    @Test
    fun autosellPrice_knownItem_returnsCorrectPrice() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(
                fakeItemName = "seal tooth",
                fakeAutosellPrice = 75,
                fakeNpcPrice = 0
            )
        )
        assertEquals("75",
            outputLib(lib, """print(to_string(autosell_price(to_item("seal tooth"))));"""))
    }

    @Test
    fun autosellPrice_unknownItem_returnsZero() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(
                fakeItemName = "seal tooth",
                fakeAutosellPrice = 75,
                fakeNpcPrice = 0
            )
        )
        assertEquals("0",
            outputLib(lib, """print(to_string(autosell_price(to_item("no such item"))));"""))
    }

    @Test
    fun npcPrice_knownItem_returnsCorrectPrice() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(
                fakeItemName = "anti-anti-antidote",
                fakeAutosellPrice = 5,
                fakeNpcPrice = 42
            )
        )
        assertEquals("42",
            outputLib(lib, """print(to_string(npc_price(to_item("anti-anti-antidote"))));"""))
    }

    @Test
    fun npcPrice_itemNotSoldByNpcs_returnsZero() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(
                fakeItemName = "anti-anti-antidote",
                fakeAutosellPrice = 5,
                fakeNpcPrice = 42
            )
        )
        assertEquals("0",
            outputLib(lib, """print(to_string(npc_price(to_item("some other item"))));"""))
    }

    @Test
    fun historicalPrice_readsFromMallPriceManager() {
        val clock = MallPriceManager.TestClock(5_000L)
        val priceManager = MallPriceManager(clock)
        priceManager.cachePrice(9_000_042, 250L, 3, 99)
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(fakeItemName = "seal tooth"),
            mallPriceManager = priceManager
        )
        assertEquals("250",
            outputLib(lib, """print(to_string(historical_price(to_item("seal tooth"))));"""))
    }

    @Test
    fun historicalAge_readsFractionalDaysSinceCached() {
        val clock = MallPriceManager.TestClock(5_000L)
        val priceManager = MallPriceManager(clock)
        // Stay within session TTL (3600s) so the cache remains readable.
        priceManager.cachePrice(9_000_042, 250L, 3, 99)
        clock.nowSeconds = 5_000L + 1_800L // 0.5 hour → 1800/86400 days
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(fakeItemName = "seal tooth"),
            mallPriceManager = priceManager
        )
        val out = outputLib(lib, """print(to_string(historical_age(to_item("seal tooth"))));""")
        assertEquals("0.020833333333333332", out) // 1800/86400
    }

    @Test
    fun historicalAge_unknownItem_isInfinity() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(fakeItemName = "seal tooth"),
            mallPriceManager = MallPriceManager(MallPriceManager.TestClock(1_000L)),
        )
        val out = outputLib(lib, """print(to_string(historical_age(to_item("no such item"))));""")
        assertTrue(out.contains("Infinity") || out == "Infinity", "expected Infinity, got $out")
    }

    @Test
    fun mallPrice_maxAgeDays_returnsCachedWhenFresh() {
        val clock = MallPriceManager.TestClock(10_000L)
        val priceManager = MallPriceManager(clock)
        priceManager.cachePrice(9_000_042, 777L, 1, 1)
        clock.nowSeconds = 10_000L + 3_600L // 1 hour later → 1/24 day
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(fakeItemName = "seal tooth"),
            mallPriceManager = priceManager,
        )
        assertEquals("777",
            outputLib(lib, """print(to_string(mall_price(to_item("seal tooth"), 1.0)));"""))
    }

    @Test
    fun retrievePrice_picksCheaperOfMallAndNpc() {
        val db = StubPricingDatabase(
            fakeItemName = "seal tooth",
            fakeAutosellPrice = 10,
            fakeNpcPrice = 100,
        )
        val mall = object : MallManager(
            MallSearchRequest(HttpClient(MockEngine { respond("[]") })),
            MallPurchaseRequest(HttpClient(MockEngine { respond("") })),
            db,
        ) {
            override suspend fun cheapestPrice(itemName: String): Long =
                if (itemName.equals("seal tooth", ignoreCase = true)) 75L else -1L
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, mallManager = mall)
        assertEquals("75",
            outputLib(lib, """print(to_string(retrieve_price(to_item("seal tooth"))));"""))
    }

    @Test
    fun retrievePrice_returnsNegativeOneWhenNoSource() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(fakeItemName = "seal tooth")
        )
        assertEquals("-1",
            outputLib(lib, """print(to_string(retrieve_price(to_item("seal tooth"))));"""))
    }
}
