package net.sourceforge.kolmafia.familiar

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FamiliarManagerTest {

    // Note: Verify exact field names against live KoL API before shipping.
    private val familiarsJson = """
        [
          {"id": 5, "name": "Mr. Wiggles", "race": "Grue", "weight": 10, "exp": 150, "kills": 3, "active": true},
          {"id": 12, "name": "Fluffy", "race": "Bunny", "weight": 7, "exp": 50, "kills": 1, "active": false}
        ]
    """.trimIndent()

    private fun makeManager(): FamiliarManager {
        val engine = MockEngine { request ->
            when {
                request.url.parameters["what"] == "familiars" ->
                    respond(familiarsJson, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("familiar.php") ->
                    respond("", HttpStatusCode.OK)
                request.url.encodedPath.contains("hatchery.php") ->
                    respond("", HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return FamiliarManager(client, GameEventBus())
    }

    @Test
    fun fetchFamiliars_setsActiveFamiliar() = runTest {
        val manager = makeManager()
        manager.fetchFamiliars()
        assertNotNull(manager.state.value.activeFamiliar)
        assertEquals("Mr. Wiggles", manager.state.value.activeFamiliar!!.name)
    }

    @Test
    fun fetchFamiliars_loadsAllFamiliars() = runTest {
        val manager = makeManager()
        manager.fetchFamiliars()
        assertEquals(2, manager.state.value.ownedFamiliars.size)
    }

    @Test
    fun initialState_hasNoActiveFamiliar() {
        val manager = makeManager()
        assertNull(manager.state.value.activeFamiliar)
    }
}
