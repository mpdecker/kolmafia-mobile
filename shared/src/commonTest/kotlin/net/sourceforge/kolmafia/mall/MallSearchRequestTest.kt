package net.sourceforge.kolmafia.mall

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MallSearchRequestTest {

    // HTML with store IDs and prices in KoL mall format
    private val searchHtml = """
        <html><body>
        <a href="mallstore.php?whichstore=12345">Bob's Shop</a>
        <b>500</b> Meat<br>
        Quantity: 3<br>
        <a href="mallstore.php?whichstore=67890">Alice's Shop</a>
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
}
