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
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.MindControlRequest
import net.sourceforge.kolmafia.request.ModeableRequest

class GameRuntimeLibraryAshP550Test {

    @Test
    fun revision_phase550() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun jillcandle_aliasesLedcandle() {
        val modes = mutableListOf<Pair<Modeable, String>>()
        val prefs = Preferences(MapSettings())
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
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
        val lib = GameRuntimeLibrary(modeableRequest = request, preferences = prefs)
        outputLib(lib, """cli_execute("jillcandle disco");""")
        assertEquals(listOf(Modeable.LED_CANDLE to "disco"), modes)
        modes.clear()
        outputLib(lib, """cli_execute("ledcandle disco");""")
        assertEquals(listOf(Modeable.LED_CANDLE to "disco"), modes)
    }

    @Test
    fun mindControl_aliasesMcd() {
        val levels = mutableListOf<Int>()
        val character = KoLCharacter()
        val request = object : MindControlRequest(
            client = HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun setLevel(level: Int): Result<Unit> {
                levels += level
                return Result.success(Unit)
            }
        }
        val lib = GameRuntimeLibrary(mindControlRequest = request, character = character)
        outputLib(lib, """cli_execute("mind-control 5");""")
        assertEquals(listOf(5), levels)
        levels.clear()
        outputLib(lib, """cli_execute("mcd 7");""")
        assertEquals(listOf(7), levels)
    }

    @Test
    fun cheat_aliasesPlay() {
        val playOut = outputLib(GameRuntimeLibrary(), """cli_execute("play");""")
        val cheatOut = outputLib(GameRuntimeLibrary(), """cli_execute("cheat");""")
        assertEquals(playOut, cheatOut)
        assertTrue(playOut.contains("Play what?", ignoreCase = true))
    }

    @Test
    fun help_listsAliases() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help");""")
        val lines = out.lines().map { it.trim() }
        assertTrue(lines.any { it == "jillcandle" })
        assertTrue(lines.any { it == "mind-control" })
        assertTrue(lines.any { it == "cheat" })
    }
}
