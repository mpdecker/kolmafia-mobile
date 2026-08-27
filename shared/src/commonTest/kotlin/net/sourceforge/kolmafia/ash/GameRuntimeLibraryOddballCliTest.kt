package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences

/** Phases 1053–1062 Oddball CLI Track E corpus. */
class GameRuntimeLibraryOddballCliTest {

    private fun invWith(items: Map<Int, InventoryItem>): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(InventoryState(items = items))
            override val state = flow.asStateFlow()
        }

    @Test
    fun skeeball_statusWithoutHttp() {
        val inv = invWith(
            mapOf(4621 to InventoryItem(4621, "Game Grid token", 3, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()), inventoryManager = inv)
        val out = outputLib(lib, """cli_execute("skeeball");""")
        assertTrue(out.contains("Usage: skeeball"), out)
        assertTrue(out.contains("3"), out)
    }

    @Test
    fun vise_requiresHashingVise() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(lib, """cli_execute("vise cyburger");""")
        assertTrue(out.contains("hashing vise"), out)
    }

    @Test
    fun throw_requiresRecipient() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(lib, """cli_execute("throw cream pie");""")
        assertTrue(out.contains("recipient"), out)
    }

    @Test
    fun buffbot_stubMentionsRequestPath() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("buffbot 5");""")
        assertTrue(out.contains("not available"), out)
        assertTrue(out.contains("buff <bot>"), out)
    }

    @Test
    fun crimbotrain_requiresPlayer() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(lib, """cli_execute("crimbotrain");""")
        assertTrue(out.contains("Train whom"), out)
    }

    @Test
    fun badmoon_reportsSignAndPrefs() {
        val p = Preferences(MapSettings())
        p.setBoolean("badMoonEncounter01", true)
        val char = KoLCharacter()
        char.setZodiacSign("Bad Moon")
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        val out = outputLib(lib, """cli_execute("badmoon");""")
        assertTrue(out.contains("Bad Moon"), out)
        assertTrue(out.contains("In Bad Moon: true"), out)
        assertTrue(out.contains("badMoonEncounter01: have"), out)
        assertTrue(out.contains("badMoonEncounter02: NEED"), out)
        assertTrue(out.contains("1 / 48"), out)
    }

    @Test
    fun flicker_statusTable() {
        val p = Preferences(MapSettings())
        p.setBoolean("flickeringPixel3", true)
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("flicker");""")
        assertTrue(out.contains("Stupid Pipes"), out)
        assertTrue(out.contains("Snakes"), out)
        assertTrue(out.contains("have"), out)
        assertTrue(out.contains("NEED"), out)
    }

    @Test
    fun help_listsOddballVerbs() {
        val lib = GameRuntimeLibrary()
        for (verb in listOf("skeeball", "vise", "throw", "buffbot", "crimbotrain", "badmoon", "flicker", "beach")) {
            val out = outputLib(lib, """cli_execute("help $verb");""")
            assertTrue(out.contains(verb), "help missing $verb: $out")
        }
    }
}
