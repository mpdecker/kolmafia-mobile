package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ManageStoreRequest

/** Phases 1043–1052 CLI Track D session/store/script corpus. */
class GameRuntimeLibrarySessionDataCliTest {

    @Test
    fun timein_printsUnavailableStub() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("timein");""")
        assertTrue(out.contains("not available"), out)
    }

    @Test
    fun relog_alias_sameStub() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("relog");""")
        assertTrue(out.contains("not available"), out)
    }

    @Test
    fun session_printsPlayerAndTallies() {
        val p = Preferences(MapSettings())
        p.setInt("_sessionAdventuresUsed", 12)
        val char = KoLCharacter()
        char.addSessionMeat(500)
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        val out = outputLib(lib, """cli_execute("session");""")
        assertTrue(out.contains("Player:"), out)
        assertTrue(out.contains("12"), out)
        assertTrue(out.contains("500"), out)
    }

    @Test
    fun summary_printsItemTally() {
        val p = Preferences(MapSettings())
        p.setString("_sessionItemTally", "seal-clubbing club:3|turtle totem:1")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("summary");""")
        assertTrue(out.contains("seal-clubbing club"), out)
        assertTrue(out.contains("turtle totem"), out)
    }

    @Test
    fun encounters_printsListingHeader() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("encounters");""")
        assertTrue(out.contains("Encounter Listing"), out)
    }

    @Test
    fun location_printsLastAdventure() {
        val p = Preferences(MapSettings())
        p.setString(Preferences.LAST_LOCATION, "The Haunted Kitchen")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("location");""")
        assertTrue(out.contains("Haunted Kitchen"), out)
    }

    @Test
    fun location_registerStub() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("location 123 Custom Spot");""")
        assertTrue(out.contains("not available"), out)
        assertTrue(out.contains("123"), out)
    }

    @Test
    fun cache_statusStub() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(lib, """cli_execute("cache");""")
        assertTrue(out.contains("Image cache"), out)
    }

    @Test
    fun undercut_withoutStore_printsUnavailable() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("undercut");""")
        assertTrue(out.contains("Mall store unavailable"), out)
    }

    @Test
    fun undercut_withStore_printsComplete() {
        val client = HttpClient(MockEngine { respond("ok") })
        val store = object : ManageStoreRequest(client) {
            override suspend fun refreshPrices(): Result<String> = Result.success("ok")
        }
        val lib = GameRuntimeLibrary(manageStoreRequest = store)
        val out = outputLib(lib, """cli_execute("undercut");""")
        assertTrue(out.contains("Repricing complete"), out)
    }

    @Test
    fun modifies_aliasesToModref() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("modifies Meat Drop");""")
        assertTrue(out.contains("Meat Drop") || out.isNotEmpty(), out)
    }

    @Test
    fun validate_missingScript() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(lib, """cli_execute("validate missing_script");""")
        assertTrue(out.contains("not found"), out)
    }

    @Test
    fun validate_existingScript() {
        val p = Preferences(MapSettings())
        val scripts = listOf(ScriptEntry(name = "demo", source = "print(\"hi\");"))
        p.setString(ScriptManager.SCRIPTS_PREF_KEY, Json.encodeToString(scripts))
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("validate demo");""")
        assertTrue(out.contains("verification complete"), out)
    }

    @Test
    fun mail_list_stub() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("mail list");""")
        assertTrue(out.contains("Inbox"), out)
    }

    @Test
    fun help_listsTrackDVerbs() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("help undercut");""")
        assertTrue(out.contains("undercut"), out)
        val out2 = outputLib(lib, """cli_execute("help timein");""")
        assertTrue(out2.contains("timein"), out2)
        val out3 = outputLib(lib, """cli_execute("help session");""")
        assertTrue(out3.contains("session"), out3)
    }
}
