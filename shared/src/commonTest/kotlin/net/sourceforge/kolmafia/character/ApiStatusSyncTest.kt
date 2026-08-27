package net.sourceforge.kolmafia.character

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

class ApiStatusSyncTest {

    private fun mockClient(): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler {
                respond(
                    content = "{}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
    }

    @Test
    fun parseStatus_effectsLastadvAndCoolitems() {
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hardcore = "0", roninleft = "0"))
        val prefs = Preferences(MapSettings())
        val effects = EffectManager(mockClient(), GameEventBus())
        val json = """
            {
              "name": "Tester",
              "hardcore": "0",
              "roninleft": "0",
              "effects": {
                "abc123": ["Goofball Effects", "10"]
              },
              "lastadv": {
                "id": "100",
                "name": "The Outskirts of Cobb's Knob",
                "link": "adventure.php?snarfblat=100",
                "container": "adventure.php"
              },
              "noncomforcers": ["clara", "spikolodon"],
              "coolitems": "spacegate,floristfriar",
              "familiar_wellfed": "1",
              "familiarpic": "familiar1",
              "famlevel": "14",
              "equipment": {
                "hat": "0",
                "weapon": "0"
              }
            }
        """.trimIndent()
        assertTrue(
            ApiStatusSync.parseStatus(
                responseText = json,
                character = char,
                preferences = prefs,
                effectManager = effects,
            ),
        )
        assertEquals("The Outskirts of Cobb's Knob", prefs.getString("lastAdventure", ""))
        assertTrue(prefs.getBoolean(Preferences.Keys.NONCOMBAT_FORCER_ACTIVE, false))
        assertEquals("clara|spikolodon", prefs.getString("noncombatForcers", ""))
        assertTrue(prefs.getBoolean("spacegateAlways", false))
        assertTrue(prefs.getBoolean("ownsFloristFriar", false))
        assertTrue(char.state.value.familiarWellFed)
        assertEquals(14, char.state.value.familiarWeight)
        assertEquals("familiar1.gif", char.state.value.familiarImage)
        assertEquals(1, effects.state.value.effects.size)
        assertEquals("Goofball Effects", effects.state.value.effects.first().name)
        assertEquals(10, effects.state.value.effects.first().duration)
    }

    @Test
    fun parseZootGrafts() {
        val char = KoLCharacter()
        val prefs = Preferences(MapSettings())
        val json = """{"grafts":{"1":"5","4":"12","5":"0"}}"""
        ApiStatusSync.parseStatus(json, char, prefs)
        assertEquals(5, prefs.getInt("zootGraftedHeadFamiliar", 0))
        assertEquals(12, prefs.getInt("zootGraftedHandLeftFamiliar", 0))
        assertEquals(0, prefs.getInt("zootGraftedHandRightFamiliar", -1))
    }
}
