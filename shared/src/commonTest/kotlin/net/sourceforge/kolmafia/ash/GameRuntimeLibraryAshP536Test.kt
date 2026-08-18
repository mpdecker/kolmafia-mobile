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

class GameRuntimeLibraryAshP536Test {

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
    fun revision_phase538() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun logout_callsSessionManagerLogout() {
        var called = false
        val session = recordingSession { called = true }
        outputLib(
            GameRuntimeLibrary(sessionManager = session),
            """cli_execute("logout");""",
        )
        assertTrue(called)
    }

    @Test
    fun help_listsLogout() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help logout");""")
        assertTrue(out.lines().any { it.trim() == "logout" })
    }
}
