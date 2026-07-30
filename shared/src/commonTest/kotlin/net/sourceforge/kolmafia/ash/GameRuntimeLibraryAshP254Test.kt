package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP254Test {

    @Test
    fun familiarBracket_pokefamAndDailyCaps() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setInt("_absintheDrops", 2)
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals("absinthe", outputLib(lib, """print(to_familiar("Green Pixie")["drop_name"]);""").trim())
        assertEquals("2", outputLib(lib, """print(to_familiar("Green Pixie")["drops_today"]);""").trim())
        assertEquals("5", outputLib(lib, """print(to_familiar("Green Pixie")["drops_limit"]);""").trim())
        assertEquals("Bite", outputLib(lib, """print(to_familiar("Angry Goat")["poke_move_1"]);""").trim())
        assertEquals("1", outputLib(lib, """print(to_familiar("Angry Goat")["poke_level_2_power"]);""").trim())
    }

    @Test
    fun familiarBracket_ownedExperience() = runBlocking {
        val db = GameDatabase()
        db.load()
        val client = HttpClient(MockEngine { respond("ok") })
        val famMgr = FamiliarManager(client, GameEventBus())
        famMgr.testSetState(
            FamiliarState(
                ownedFamiliars = listOf(
                    FamiliarData(
                        id = 1,
                        name = "Mosquito",
                        race = "Mosquito",
                        weight = 5,
                        experience = 42,
                        kills = 0,
                    ),
                ),
            ),
        )
        val lib = GameRuntimeLibrary(gameDatabase = db, familiarManager = famMgr)
        assertEquals("Mosquito", outputLib(lib, """print(to_familiar("Mosquito")["name"]);""").trim())
        assertEquals("42", outputLib(lib, """print(to_familiar("Mosquito")["experience"]);""").trim())
        assertEquals("", outputLib(lib, """print(to_familiar("Leprechaun")["name"]);""").trim())
    }

    @Test
    fun familiarBracket_fightLimits() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setInt("_hipsterAdv", 4)
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals("4", outputLib(lib, """print(to_familiar("Mini-Hipster")["fights_today"]);""").trim())
        assertEquals("7", outputLib(lib, """print(to_familiar("Mini-Hipster")["fights_limit"]);""").trim())
    }
}
