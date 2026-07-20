package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase

class GameRuntimeLibraryAshP47Test {

    @Test
    fun allMonstersWithId_countMatchesDatabase() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        // Map is keyed by monster name; duplicate names collapse (desktop keys by id Value).
        val expected = MonsterDatabase.byId.values
            .filter { it.id != 0 }
            .map { it.name }
            .toSet()
            .size
        assertTrue(expected > 100)
        assertEquals(
            expected.toString(),
            outputLib(lib, """print(count(all_monsters_with_id()));""").trim(),
        )
    }

    @Test
    fun allMonstersWithId_includesHugeMosquito() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "true",
            outputLib(
                lib,
                """print(to_string(all_monsters_with_id()[to_monster("huge mosquito")]));""",
            ).trim(),
        )
    }

    @Test
    fun allMonstersWithId_excludesNone() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        // Unresolved none → empty monster key defaults to false when absent
        assertEquals(
            "false",
            outputLib(
                lib,
                """print(to_string(all_monsters_with_id()[to_monster("none")]));""",
            ).trim(),
        )
    }
}
