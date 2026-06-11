package net.sourceforge.kolmafia.ash

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
import kotlin.test.Test
import kotlin.test.assertEquals

/** Minimal GameDatabase stub that returns controlled items and NPC prices without loading from disk. */
private class StubPricingDatabase(
    private val fakeItemName: String? = null,
    private val fakeAutosellPrice: Int = 0,
    private val fakeNpcPrice: Int = 0,
) : GameDatabase() {

    private val fakeItem: ItemData? = fakeItemName?.let {
        ItemData(
            id = 1,
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
        priceManager.cachePrice(1, 250L, 3, 99)
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(fakeItemName = "seal tooth"),
            mallPriceManager = priceManager
        )
        assertEquals("250",
            outputLib(lib, """print(to_string(historical_price(to_item("seal tooth"))));"""))
    }

    @Test
    fun historicalAge_readsSecondsSinceCached() {
        val clock = MallPriceManager.TestClock(5_000L)
        val priceManager = MallPriceManager(clock)
        priceManager.cachePrice(1, 250L, 3, 99)
        clock.nowSeconds = 5_120L
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(fakeItemName = "seal tooth"),
            mallPriceManager = priceManager
        )
        assertEquals("120",
            outputLib(lib, """print(to_string(historical_age(to_item("seal tooth"))));"""))
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
