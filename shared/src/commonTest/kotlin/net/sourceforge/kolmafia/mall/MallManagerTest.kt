package net.sourceforge.kolmafia.mall

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import kotlin.test.*

// Two offers: shopId=1 price=100 qty=5, shopId=2 price=200 qty=5 (KoL mall HTML format)
private val SEARCH_HTML = """
    <html><body>
    <a href="mallstore.php?whichstore=1">Shop One</a>
    <input type="hidden" name="whichitem" value="42">
    <b>100</b> Meat<br>
    Quantity: 5<br>
    <a href="mallstore.php?whichstore=2">Shop Two</a>
    <input type="hidden" name="whichitem" value="42">
    <b>200</b> Meat<br>
    Quantity: 5<br>
    </body></html>
""".trimIndent()

private val BUY_HTML = "<html>You acquire: test widget</html>"

private fun stubDb(): GameDatabase = object : GameDatabase() {
    override fun item(id: Int) = if (id == 42) ItemData(
        id = 42, name = "test widget", descId = "", image = "",
        primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
        access = setOf('t', 'd'), autosellPrice = 10, plural = null
    ) else null
    override fun item(name: String) = if (name == "test widget") item(42) else null
}

private fun buildEngine(searchHtml: String = SEARCH_HTML, buyHtml: String = BUY_HTML): MockEngine {
    return MockEngine { request ->
        val body = request.body.toByteArray().decodeToString()
        if (body.contains("searchmall")) respond(searchHtml, HttpStatusCode.OK)
        else respond(buyHtml, HttpStatusCode.OK)
    }
}

class MallManagerTest {

    @Test
    fun buy_purchasesRequestedCount() = runTest {
        val engine = buildEngine()
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        assertEquals(3, manager.buy(itemId = 42, count = 3))
    }

    @Test
    fun buy_respectsMaxPrice_skipsExpensiveOffers() = runTest {
        val engine = buildEngine()
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        // maxPrice=150 → only the 100-meat shop qualifies (qty=5), so 3 can be filled
        assertEquals(3, manager.buy(itemId = 42, count = 3, maxPrice = 150))
    }

    @Test
    fun buy_noListingsForItem_returnsZero() = runTest {
        val engine = buildEngine(searchHtml = "<html><body>No results.</body></html>")
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        assertEquals(0, manager.buy(itemId = 42, count = 1))
    }

    @Test
    fun buy_unknownItemId_returnsZero() = runTest {
        val engine = buildEngine()
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        // itemId 999 not in stubDb → no item name resolved → early return 0
        assertEquals(0, manager.buy(itemId = 999, count = 1))
    }

    @Test
    fun cheapestPrice_returnsLowestListedPrice() = runTest {
        val engine = buildEngine()
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        assertEquals(100L, manager.cheapestPrice("test widget"))
    }

    @Test
    fun cheapestPrice_noListings_returnsMinusOne() = runTest {
        val engine = buildEngine(searchHtml = "<html><body>No results.</body></html>")
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        assertEquals(-1L, manager.cheapestPrice("test widget"))
    }

    @Test
    fun cheapestPrice_cachesPriceInMallPriceManager() = runTest {
        val engine = buildEngine()
        val client = HttpClient(engine)
        val clock = MallPriceManager.TestClock(1_000L)
        val priceManager = MallPriceManager(clock)
        val manager = MallManager(
            MallSearchRequest(client), MallPurchaseRequest(client), stubDb(), priceManager
        )
        assertEquals(100L, manager.cheapestPrice("test widget"))
        assertEquals(100L, priceManager.getHistoricalPrice(42))
        assertEquals(0L, priceManager.getHistoricalAge(42))
    }
}
