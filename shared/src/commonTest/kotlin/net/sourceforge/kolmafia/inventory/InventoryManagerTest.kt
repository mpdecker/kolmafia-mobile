package net.sourceforge.kolmafia.inventory

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.event.GameEventBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryManagerTest {

    private val inventoryJson = """{"3": 5, "43": 2}"""
    private val equipmentJson = """{}"""

    private fun makeManager(
        invJson: String = inventoryJson,
        equipJson: String = equipmentJson
    ): InventoryManager {
        val engine = MockEngine { request ->
            when {
                request.url.parameters["what"] == "inventory" ->
                    respond(invJson, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.parameters["what"] == "equipment" ->
                    respond(equipJson, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return InventoryManager(client, GameEventBus())
    }

    @Test
    fun fetchInventory_populatesItems() = runTest {
        val manager = makeManager()
        manager.fetchInventory()
        val state = manager.state.value
        assertEquals(2, state.items.size)
        assertEquals(5, state.items[3]?.quantity)
        assertEquals(2, state.items[43]?.quantity)
    }

    @Test
    fun initialState_isEmpty() {
        val manager = makeManager()
        assertTrue(manager.state.value.items.isEmpty())
    }

    @Test
    fun fetchInventory_clearsStaleFlag() = runTest {
        val manager = makeManager()
        manager.fetchInventory()
        assertFalse(manager.state.value.isStale)
    }
}
