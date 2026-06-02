package net.sourceforge.kolmafia.effect

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

class EffectManagerTest {

    // api.php?what=effects returns a JSON object keyed by effect ID string.
    private val effectsJson = """
        {
          "1":   {"name": "On the Trail",      "duration": 10},
          "189": {"name": "Musk of the Moose", "duration": 5}
        }
    """.trimIndent()

    private fun makeManager(effectsResponse: String = effectsJson): EffectManager {
        val engine = MockEngine { request ->
            when {
                request.url.parameters["what"] == "effects" ->
                    respond(effectsResponse, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return EffectManager(client, GameEventBus())
    }

    @Test
    fun fetchEffects_populatesEffectList() = runTest {
        val manager = makeManager()
        manager.fetchEffects()
        val state = manager.state.value
        assertEquals(2, state.effects.size)
        assertFalse(state.isStale)
    }

    @Test
    fun fetchEffects_parsesNamesAndDurations() = runTest {
        val manager = makeManager()
        manager.fetchEffects()
        val effects = manager.state.value.effects
        val trail = effects.find { it.id == 1 }
        val moose = effects.find { it.id == 189 }
        assertEquals("On the Trail", trail?.name)
        assertEquals(10, trail?.duration)
        assertEquals("Musk of the Moose", moose?.name)
        assertEquals(5, moose?.duration)
    }

    @Test
    fun fetchEffects_emptyResponse_clearsEffects() = runTest {
        val manager = makeManager(effectsResponse = "{}")
        manager.fetchEffects()
        assertTrue(manager.state.value.effects.isEmpty())
        assertFalse(manager.state.value.isStale)
    }

    @Test
    fun initialState_isEmpty() {
        val manager = makeManager()
        assertTrue(manager.state.value.effects.isEmpty())
    }
}
