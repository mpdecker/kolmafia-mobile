package net.sourceforge.kolmafia.inventory

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.event.GameEventBus
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InventoryRefreshAndParseTest {

    @AfterTest
    fun tearDown() {
        InventoryRefresh.clearListeners()
    }

    @Test
    fun fireInventoryChanged_notifiesListeners() {
        var count = 0
        InventoryRefresh.addListener { count++ }
        InventoryRefresh.fireInventoryChanged()
        InventoryRefresh.fireInventoryChanged()
        assertEquals(2, count)
    }

    @Test
    fun parseInventory_resolvesNamesAndSkipsZero() = runTest {
        val manager = InventoryManager(
            HttpClient(MockEngine {
                respond(
                    """{"3":5,"99":0}""",
                    HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }),
            GameEventBus(),
        )
        val parsed = manager.parseInventory(mapOf("3" to 5, "99" to 0, "bad" to 1))
        assertEquals(1, parsed.size)
        assertEquals(5, parsed[3]?.quantity)
        assertTrue(manager.checkItem(3).not()) // not applied yet
        manager.applyParsedInventory(parsed)
        assertTrue(manager.checkItem(3))
        assertEquals(5, manager.getCount(3))
    }

    @Test
    fun gainItemLocally_firesRefresh() {
        val manager = InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        )
        var fired = false
        InventoryRefresh.addListener { fired = true }
        manager.gainItemLocally(7, 2)
        assertTrue(fired)
        assertEquals(2, manager.getCount(7))
    }
}
