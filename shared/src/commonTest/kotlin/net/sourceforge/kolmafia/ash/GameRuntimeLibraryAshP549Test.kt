package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.HorseryRequest
import net.sourceforge.kolmafia.request.ModeableRequest

class GameRuntimeLibraryAshP549Test {

    @Test
    fun revision_phase550() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun bare_umbrella_printsCurrentState() {
        val prefs = Preferences(MapSettings())
        prefs.setString("umbrellaState", "bucket style")
        val out = outputLib(
            GameRuntimeLibrary(
                preferences = prefs,
                modeableRequest = ModeableRequest(
                    client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                    choiceRequest = ChoiceRequest(HttpClient(MockEngine { respond("ok") })),
                    preferences = prefs,
                ),
            ),
            """cli_execute("umbrella");""",
        )
        assertTrue(out.isNotBlank())
        assertTrue(out.contains("bucket style", ignoreCase = true) || out.contains("umbrella", ignoreCase = true))
        assertTrue(out.contains("Available", ignoreCase = true) || out.contains("Usage", ignoreCase = true))
    }

    @Test
    fun bare_horsery_printsCurrentState() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_horsery", "normal horse")
        val out = outputLib(
            GameRuntimeLibrary(preferences = prefs),
            """cli_execute("horsery");""",
        )
        assertTrue(out.isNotBlank())
        assertTrue(out.contains("normal horse", ignoreCase = true))
    }

    @Test
    fun umbrella_set_stillWorks() {
        val prefs = Preferences(MapSettings())
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val modes = mutableListOf<Pair<Modeable, String>>()
        val request = object : ModeableRequest(
            client = HttpClient(engine),
            choiceRequest = ChoiceRequest(HttpClient(engine)),
            preferences = prefs,
        ) {
            override suspend fun setMode(modeable: Modeable, mode: String): Result<Unit> {
                modes += modeable to mode
                return Result.success(Unit)
            }
        }
        val out = outputLib(
            GameRuntimeLibrary(modeableRequest = request, preferences = prefs),
            """cli_execute("umbrella bucket");""",
        )
        assertEquals(listOf(Modeable.UMBRELLA to "bucket style"), modes)
        assertTrue(out.contains("umbrella", ignoreCase = true))
    }

    @Test
    fun horsery_set_stillWorks() {
        val rides = mutableListOf<String>()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("horseryAvailable", true)
        val request = object : HorseryRequest(
            client = HttpClient(MockEngine { respond("ok") }),
            choiceRequest = ChoiceRequest(HttpClient(MockEngine { respond("ok") })),
            preferences = prefs,
        ) {
            override suspend fun ride(horseName: String): Result<Unit> {
                rides += horseName
                return Result.success(Unit)
            }
        }
        outputLib(
            GameRuntimeLibrary(horseryRequest = request, preferences = prefs),
            """cli_execute("horsery pale");""",
        )
        assertEquals(listOf("pale"), rides)
    }
}
