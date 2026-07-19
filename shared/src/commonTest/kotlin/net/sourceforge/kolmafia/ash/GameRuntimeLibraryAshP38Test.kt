package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP38Test {

    @Test
    fun getMonsters_returnsPositiveWeightMonsters() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val expected = CombatDatabase.getByLocation("The Spooky Forest")!!
            .monsters.count { it.weight > 0 }
        assertEquals(
            expected.toString(),
            outputLib(lib, """print(count(get_monsters(to_location("The Spooky Forest"))));""").trim(),
        )
        assertTrue(
            outputLib(lib, """print(get_monsters(to_location("The Spooky Forest"))[0]);""").trim()
                .isNotEmpty(),
        )
        // Banished/zero-weight entries excluded (Baiowulf:-1, Headless Horseman:0)
        assertEquals("6", expected.toString())
    }

    @Test
    fun appearanceRates_weightBasedRatesAndNoneKey() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "15.0",
            outputLib(
                lib,
                """print(to_string(appearance_rates(to_location("The Spooky Forest"))[to_monster("none")]));""",
            ).trim(),
        )
        // 6 equal-weight monsters at 85% combat → 85/6 each
        val rate = outputLib(
            lib,
            """print(to_string(appearance_rates(to_location("The Spooky Forest"))[to_monster("spooky vampire")]));""",
        ).trim().toDouble()
        assertEquals(85.0 / 6.0, rate, absoluteTolerance = 0.0001)
    }

    @Test
    fun appearanceRates_includeQueueOverload_matchesStateless() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val withQueue = outputLib(
            lib,
            """print(to_string(appearance_rates(to_location("The Spooky Forest"), true)[to_monster("none")]));""",
        ).trim()
        val without = outputLib(
            lib,
            """print(to_string(appearance_rates(to_location("The Spooky Forest"))[to_monster("none")]));""",
        ).trim()
        assertEquals(without, withQueue)
    }

    @Test
    fun getLocationMonsters_marksPositiveWeightTrue() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "true",
            outputLib(
                lib,
                """print(to_string(get_location_monsters(to_location("The Spooky Forest"))[to_monster("spooky vampire")]));""",
            ).trim(),
        )
        assertEquals(
            "false",
            outputLib(
                lib,
                """print(to_string(get_location_monsters(to_location("The Spooky Forest"))[to_monster("Baiowulf")]));""",
            ).trim(),
        )
    }

    @Test
    fun monsterBracketParts_readsMonsterPartsDatabase() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "4",
            outputLib(lib, """print(count(to_monster("spooky vampire")["parts"]));""").trim(),
        )
        assertEquals(
            "arm",
            outputLib(lib, """print(to_monster("spooky vampire")["parts"][0]);""").trim(),
        )
        assertEquals(
            "head",
            outputLib(lib, """print(to_monster("spooky vampire")["parts"][1]);""").trim(),
        )
    }

    @Test
    fun monsterBracketParts_unknownMonster_returnsEmpty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(lib, """print(count(to_monster("nonexistent critter")["parts"]));""").trim(),
        )
    }
}
