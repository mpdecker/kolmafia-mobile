package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP31Test {

    private fun libWithDb(): GameRuntimeLibrary = runBlocking {
        val db = GameDatabase()
        db.load()
        GameRuntimeLibrary(gameDatabase = db)
    }

    @Test
    fun toLocation_resolvesKnownLocation() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "The Spooky Forest",
            outputLib(lib, """print(to_location("spooky forest"));""").trim(),
        )
        assertEquals("The Spooky Forest", outputLib(lib, """print(to_location("20"));""").trim())
        assertEquals("", outputLib(lib, """print(to_location("bogus"));""").trim())
    }

    @Test
    fun locationIsValid_stillLiveFromAshP11() = runBlocking {
        val lib = libWithDb()
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_valid(to_location("The Spooky Forest"))));""").trim(),
        )
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(is_valid(to_location("Not A Real Location 9999"))));""").trim(),
        )
    }

    @Test
    fun locationTypeOf_returnsLocation() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "location",
            outputLib(lib, """print(type_of(to_location("The Spooky Forest")));""").trim(),
        )
    }
}
