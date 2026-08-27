package net.sourceforge.kolmafia.mall

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.session.StoreManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MallEconomyResidualTest {
    @AfterTest
    fun reset() {
        MallPurchaseRequest.resetStoreFilters()
        StoreManager.clearCache()
    }

    @Test
    fun searchParsesDesktopRowsLimitsNamesAndGreyedStores() = runTest {
        val html = """
            Search Results:
            <table class="itemtable"><tr id="item_42"><td>
            <a onclick="descitem(123)">test widget</a>
            <tr class="graybelow"><td class="stock">8</td>
            <td><a href="mallstore.php?whichstore=111&searchitem=42&searchprice=125"><b>Cheap<br>Shop</b></a></td>
            <td>3&nbsp;/&nbsp;day</td></tr>
            <tr class="graybelow limited"><td class="stock">9</td>
            <td><a href="mallstore.php?whichstore=222&searchitem=42&searchprice=200"><b>Grey Shop</b></a></td>
            <td>2&nbsp;/&nbsp;day</td></tr>
            </td></tr></table>
        """.trimIndent()
        val request = MallSearchRequest(HttpClient(MockEngine { respond(html, HttpStatusCode.OK) }))

        val rows = request.search("\"test widget\"", 10)

        assertEquals(2, rows.size)
        assertEquals("Cheap Shop", rows[0].shopName)
        assertEquals(3, rows[0].limit)
        assertEquals(8, rows[0].quantity)
        assertFalse(rows[1].canPurchase)
    }

    @Test
    fun searchFetchesEveryResultPage() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            val page = if (calls == 1) {
                "(Items 1-30 of 31)"
            } else {
                "(Items 31-31 of 31)"
            }
            respond("$page No results.", HttpStatusCode.OK)
        }

        MallSearchRequest(HttpClient(engine)).search("", 0)

        assertEquals(2, calls)
    }

    @Test
    fun nthCheapestUsesPurchasableDailyLimits() {
        val manager = MallPriceManager(MallPriceManager.TestClock(1_000))
        val rows = listOf(
            MallListing(1, "one", 42, 100, 2, limit = 2),
            MallListing(2, "two", 42, 150, 9, limit = 9, canPurchase = false),
            MallListing(3, "three", 42, 200, 5, limit = 5),
        )

        assertEquals(200, manager.updateMallPrice(42, rows))
        assertEquals(200, manager.getMallPrice(42))
    }

    @Test
    fun savedSearchExpiresAfterSixtySecondsAndFiltersStores() {
        val clock = MallPriceManager.TestClock(1_000)
        val manager = MallPriceManager(clock)
        MallPurchaseRequest.addForbiddenStore(2)
        manager.saveMallSearch(
            42,
            listOf(
                MallListing(1, "one", 42, 100, 1),
                MallListing(2, "two", 42, 90, 1),
            ),
        )

        assertEquals(listOf(1), manager.getSavedSearch(42, 1)?.map { it.shopId })
        clock.nowSeconds += 60
        assertNull(manager.getSavedSearch(42, 1))
    }

    @Test
    fun storeManagerParsesDeetsAndMutatesInventory() {
        val html = """
            <tr class="deets" rel="1"><td>image</td><td><b>test widget</b></td>
            <td>1,081<</td><td><input name="price[42]" value="230"></td>
            <td><input name="limit[42]" value="6"></td></tr>
        """.trimIndent()

        StoreManager.update(html, StoreManager.TableType.DEETS)
        assertEquals(1081, StoreManager.shopAmount(42))
        assertEquals(230, StoreManager.getPrice(42))
        assertEquals(6, StoreManager.getLimit(42))

        StoreManager.addItem(42, 4, 250, 2)
        assertEquals(1085, StoreManager.shopAmount(42))
        StoreManager.removeItem(42, 2000)
        assertEquals(0, StoreManager.shopAmount(42))
    }

    @Test
    fun storeManagerParsesLog() {
        StoreManager.parseLog("<span class=small>08/27 Bob bought a widget<br>08/26 Ann bought two<br></span>")
        assertEquals(2, StoreManager.getStoreLog().size)
        assertEquals("2: 08/27 Bob bought a widget", StoreManager.getStoreLog()[0].toString())
    }
}
