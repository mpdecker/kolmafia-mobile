package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.DailyResourceTracker
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager

class GameRuntimeLibraryAshP544Test {

    private fun recordingSession(onLogout: () -> Unit): SessionManager {
        val client = HttpClient(MockEngine { respond("ok") })
        val bus = GameEventBus()
        val prefs = Preferences(MapSettings())
        return object : SessionManager(
            loginRequest = LoginRequest(client),
            characterRequest = CharacterRequest(client),
            character = KoLCharacter(),
            preferences = prefs,
            inventoryManager = InventoryManager(client, bus),
            familiarManager = FamiliarManager(client, bus),
            skillManager = SkillManager(client, SkillCastRequest(client), bus, prefs),
            effectManager = EffectManager(client, bus),
            scriptManager = ScriptManager(GameRuntimeLibrary(), prefs, bus),
            gameDatabase = GameDatabase(),
            dailyResourceTracker = DailyResourceTracker(),
        ) {
            override fun logout() {
                onLogout()
            }
        }
    }

    @Test
    fun revision_phase544() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun exit_callsSessionManagerLogout() {
        var called = false
        val session = recordingSession { called = true }
        outputLib(
            GameRuntimeLibrary(sessionManager = session),
            """cli_execute("exit");""",
        )
        assertTrue(called)
    }

    @Test
    fun quit_callsSessionManagerLogout() {
        var called = false
        val session = recordingSession { called = true }
        outputLib(
            GameRuntimeLibrary(sessionManager = session),
            """cli_execute("quit");""",
        )
        assertTrue(called)
    }

    @Test
    fun help_listsExitAndQuit() {
        val exitOut = outputLib(GameRuntimeLibrary(), """cli_execute("help exit");""")
        assertTrue(exitOut.lines().any { it.trim() == "exit" })
        val quitOut = outputLib(GameRuntimeLibrary(), """cli_execute("help quit");""")
        assertTrue(quitOut.lines().any { it.trim() == "quit" })
    }
}
