package net.sourceforge.kolmafia.mall

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.test.*

class MallSearchRequestTest {

    // HTML with store IDs and prices in KoL mall format
    private val searchHtml = """
        <html><body>
        <a href="mallstore.php?whichstore=12345">Bob's Shop</a>
        <input type="hidden" name="whichitem" value="799">
        <b>500</b> Meat<br>
        Quantity: 3<br>
        <a href="mallstore.php?whichstore=67890">Alice's Shop</a>
        <input type="hidden" name="whichitem" value="799">
        <b>450</b> Meat<br>
        Quantity: 10<br>
        </body></html>
    """.trimIndent()

    @Test
    fun search_parsesListingsFromHtml() = runTest {
        val engine = MockEngine { respond(searchHtml, HttpStatusCode.OK) }
        val request = MallSearchRequest(HttpClient(engine))

        val listings = request.search(itemName = "fuzzy dice", limit = 5)

        assertEquals(2, listings.size)
        assertEquals(12345, listings[0].shopId)
        assertEquals(500L, listings[0].price)
        assertEquals(3, listings[0].quantity)
        assertEquals(67890, listings[1].shopId)
        assertEquals(450L, listings[1].price)
    }

    @Test
    fun search_noResults_returnsEmpty() = runTest {
        val engine = MockEngine { respond("<html><body>No results.</body></html>", HttpStatusCode.OK) }
        val request = MallSearchRequest(HttpClient(engine))

        val listings = request.search(itemName = "zzz_nonexistent", limit = 5)

        assertTrue(listings.isEmpty())
    }

    @Test
    fun search_networkError_returnsEmpty() = runTest {
        val engine = MockEngine { throw Exception("network error") }
        val request = MallSearchRequest(HttpClient(engine))

        val listings = request.search(itemName = "anything", limit = 5)

        assertTrue(listings.isEmpty())
    }

    @Test
    fun search_respectsLimit() = runTest {
        val engine = MockEngine { respond(searchHtml, HttpStatusCode.OK) }
        val request = MallSearchRequest(HttpClient(engine))

        val listings = request.search(itemName = "fuzzy dice", limit = 1)

        assertEquals(1, listings.size)
    }

    @Test
    fun search_parsesItemId() = runTest {
        val engine = MockEngine { respond(searchHtml, HttpStatusCode.OK) }
        val listings = MallSearchRequest(HttpClient(engine)).search("fuzzy dice", limit = 5)
        assertEquals(799, listings[0].itemId)
    }

    @Test
    fun search_parsesItemDetailTableWithLimitedRow() = runTest {
        val html = """
            Search Results:
            <table class="itemtable"><tr><td class="item" id="item_799">
            <a href="javascript:descitem(123);">fuzzy dice</a>
            <tr class="graybelow limited"><td class="stock">0</td>
            0&nbsp;/&nbsp;day
            whichstore=111&searchitem=799&searchprice=500"><b>Shop</b></tr>
            <tr class="graybelow"><td class="stock">3</td>
            whichstore=222&searchitem=799&searchprice=450"><b>Other Shop</b></tr>
            </table>
        """.trimIndent()
        val rows = MallSearchRequest(HttpClient(MockEngine { respond("x", HttpStatusCode.OK) }))
            .parseMallHtml(html, limit = 10)
        // graybelow limited rows are stripped in preprocess; remaining purchasable row remains.
        assertEquals(1, rows.size)
        assertEquals(true, rows[0].canPurchase)
        assertEquals(222, rows[0].shopId)
    }

    @Test
    fun registerRequest_logsMallSearch() {
        val prefs = com.russhwolf.settings.MapSettings()
        val logger = SessionLogger(
            net.sourceforge.kolmafia.preferences.Preferences(prefs),
            net.sourceforge.kolmafia.event.GameEventBus(),
        )
        assertTrue(MallSearchRequest.registerRequest("mall.php?pudnuggler=fuzzy+dice&category=allitems", logger))
        val lines1 = logger.recentLines()
        assertTrue(lines1.any { it.contains("mallsearch") && it.contains("fuzzy dice") })
        assertTrue(MallSearchRequest.registerRequest("mallstore.php?whichstore=12345", logger))
        val lines2 = logger.recentLines()
        assertTrue(lines2.any { it.contains("mallsearch shop #12345") })
        assertFalse(MallSearchRequest.registerRequest("mallstore.php?whichstore=1&buying=1&whichitem=1.100", logger))
    }

    @Test
    fun preflight_getSearchStringDecodesEntities() {
        // Without a registered item, falls back to input; decode helper still covers data names.
        assertEquals("A & B", MallSearchPreflight.decodeEntities("A &amp; B"))
        assertEquals("\"quoted\"", MallSearchPreflight.decodeEntities("&quot;quoted&quot;"))
    }

    @Test
    fun favorites_patternExtractsStoreIds() {
        val html = """&action=unfave&whichstore=111">x&action=unfave&whichstore=222">"""
        val ids = MallSearchRequest.FAVORITES_PATTERN.findAll(html).map { it.groupValues[1].toInt() }.toList()
        assertEquals(listOf(111, 222), ids)
    }
}
