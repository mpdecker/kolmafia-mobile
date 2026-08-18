package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP68Test {

    @Test
    fun minMaxSprinkles_mosquitoDefaultZero() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(lib, """print(to_monster("huge mosquito")["min_sprinkles"]);""").trim(),
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_monster("huge mosquito")["max_sprinkles"]);""").trim(),
        )
    }

    @Test
    fun minMaxSprinkles_gingerbreadPigeonNumeric() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "1",
            outputLib(lib, """print(to_monster("gingerbread pigeon")["min_sprinkles"]);""").trim(),
        )
        assertEquals(
            "3",
            outputLib(lib, """print(to_monster("gingerbread pigeon")["max_sprinkles"]);""").trim(),
        )
    }

    @Test
    fun minMaxSprinkles_gingerbreadAlligatorExpressionDefaultPref() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "28",
            outputLib(lib, """print(to_monster("gingerbread alligator")["min_sprinkles"]);""").trim(),
        )
        assertEquals(
            "30",
            outputLib(lib, """print(to_monster("gingerbread alligator")["max_sprinkles"]);""").trim(),
        )
    }

    @Test
    fun minMaxSprinkles_unknownMonsterZero() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(lib, """print(to_monster("nonexistent critter")["min_sprinkles"]);""").trim(),
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_monster("nonexistent critter")["max_sprinkles"]);""").trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }
}
