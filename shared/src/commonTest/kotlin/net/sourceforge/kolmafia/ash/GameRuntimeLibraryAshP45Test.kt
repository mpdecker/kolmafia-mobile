package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP45Test {

    @Test
    fun meatDrop_monster_returnsBaseMeat() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "10",
            outputLib(lib, """print(meat_drop(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun meatDrop_noneMonster_returnsMinusOne() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("-1", outputLib(lib, """print(meat_drop(to_monster("none")));""").trim())
    }

    @Test
    fun meatDrop_zeroArg_readsLastMonster() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals("10", outputLib(lib, """print(meat_drop());""").trim())
    }

    @Test
    fun itemDrops_mapRates() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "30.0",
            outputLib(
                lib,
                """print(to_string(item_drops(to_monster("huge mosquito"))[to_item("delicious swamp muck")]));""",
            ).trim(),
        )
        assertEquals(
            "10.0",
            outputLib(
                lib,
                """print(to_string(item_drops(to_monster("huge mosquito"))[to_item("huge mosquito proboscis")]));""",
            ).trim(),
        )
    }

    @Test
    fun itemDrops_noneMonster_emptyMap() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(lib, """print(count(item_drops(to_monster("none"))));""").trim(),
        )
    }

    @Test
    fun itemDropsArray_recordFields() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "delicious swamp muck",
            outputLib(
                lib,
                """print(item_drops_array(to_monster("huge mosquito"))[0].drop);""",
            ).trim(),
        )
        assertEquals(
            "30.0",
            outputLib(
                lib,
                """print(to_string(item_drops_array(to_monster("huge mosquito"))[0].rate));""",
            ).trim(),
        )
        assertEquals(
            "",
            outputLib(
                lib,
                """print(item_drops_array(to_monster("huge mosquito"))[0].type);""",
            ).trim(),
        )
        assertEquals(
            "2",
            outputLib(lib, """print(count(item_drops_array(to_monster("huge mosquito"))));""").trim(),
        )
    }

    @Test
    fun itemDropsArray_zeroArg_readsLastMonster() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "delicious swamp muck",
            outputLib(lib, """print(item_drops_array()[0].drop);""").trim(),
        )
    }
}
