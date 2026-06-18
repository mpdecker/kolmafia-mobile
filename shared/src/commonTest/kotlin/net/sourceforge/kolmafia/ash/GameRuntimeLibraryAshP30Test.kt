package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase

class GameRuntimeLibraryAshP30Test {

    private fun libWithDb(): GameRuntimeLibrary = runBlocking {
        val db = GameDatabase()
        db.load()
        GameRuntimeLibrary(gameDatabase = db)
    }

    @Test
    fun toMonster_resolvesKnownMonster() = runBlocking {
        val lib = libWithDb()
        assertEquals("huge mosquito", outputLib(lib, """print(to_monster("huge mosquito"));""").trim())
        assertEquals("", outputLib(lib, """print(to_monster("bogus"));""").trim())
    }

    @Test
    fun toPath_resolvesKnownPath() = runBlocking {
        ModifierDatabase.load()
        val lib = GameRuntimeLibrary()
        assertEquals("Dark Gyffte", outputLib(lib, """print(to_path("dark gyffte"));""").trim())
        assertEquals("Teetotaler", outputLib(lib, """print(to_path("Teetotaler"));""").trim())
        assertEquals("", outputLib(lib, """print(to_path("bogus"));""").trim())
    }

    @Test
    fun toThrall_resolvesKnownThrall() = runBlocking {
        ModifierDatabase.load()
        val lib = GameRuntimeLibrary()
        assertEquals("Lasagmbie", outputLib(lib, """print(to_thrall("lasagmbie"));""").trim())
        assertEquals("", outputLib(lib, """print(to_thrall("bogus"));""").trim())
    }

    @Test
    fun monsterIsValid_stillLiveFromAshP11() = runBlocking {
        val lib = libWithDb()
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_valid(to_monster("huge mosquito"))));""").trim(),
        )
        assertEquals("false", outputLib(lib, """print(to_string(is_valid(to_monster("bogus"))));""").trim())
    }

    @Test
    fun pathTypeOf_returnsPath() = runBlocking {
        ModifierDatabase.load()
        val lib = GameRuntimeLibrary()
        assertEquals("path", outputLib(lib, """print(type_of(to_path("Dark Gyffte")));""").trim())
    }
}
