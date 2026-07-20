package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase

class GameRuntimeLibraryAshP44Test {

    @Test
    fun jumpChance_location_matchesMinHelper() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val expected = CombatAdjustment.locationJumpChance(
            locationName = "The Spooky Forest",
            initBonus = 0,
            initMl = 0,
            attackMl = 0,
            baseMainstat = 0,
            resolveMonster = { MonsterDatabase.getByName(it) },
        )
        assertEquals(
            expected.toString(),
            outputLib(
                lib,
                """print(jump_chance(to_location("The Spooky Forest"), 0, 0));""",
            ).trim(),
        )
    }

    @Test
    fun jumpChance_location_mlAffectsInitPenalty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val atZero = CombatAdjustment.locationJumpChance(
            "The Spooky Forest", 0, 0, 0, 0,
        ) { MonsterDatabase.getByName(it) }
        val atForty = CombatAdjustment.locationJumpChance(
            "The Spooky Forest", 0, 40, 0, 0,
        ) { MonsterDatabase.getByName(it) }
        assertEquals(
            atZero.toString(),
            outputLib(lib, """print(jump_chance(to_location("The Spooky Forest"), 0, 0));""").trim(),
        )
        assertEquals(
            atForty.toString(),
            outputLib(lib, """print(jump_chance(to_location("The Spooky Forest"), 0, 40));""").trim(),
        )
        // Higher init ML lowers jump chance via initPenalty
        assertTrue(atForty < atZero || atZero == 0)
    }

    @Test
    fun jumpChance_location_oneArg_usesCurrentInitAndMl() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val expected = CombatAdjustment.locationJumpChance(
            "The Spooky Forest", 0, 0, 0, 0,
        ) { MonsterDatabase.getByName(it) }
        assertEquals(
            expected.toString(),
            outputLib(lib, """print(jump_chance(to_location("The Spooky Forest")));""").trim(),
        )
    }

    @Test
    fun jumpChance_unknownLocation_returnsZero() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(lib, """print(jump_chance(to_location("Nowhere Land That Does Not Exist")));""").trim(),
        )
    }

    @Test
    fun jumpChance_location_excludesZeroWeight() = runBlocking {
        val db = GameDatabase()
        db.load()
        // Spooky Forest has Baiowulf:-1 and Headless Horseman:0 — must not affect min
        val zone = CombatDatabase.getByLocation("The Spooky Forest")!!
        assertTrue(zone.monsters.any { it.name.equals("Baiowulf", ignoreCase = true) && it.weight < 0 })
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val ash = outputLib(
            lib,
            """print(jump_chance(to_location("The Spooky Forest"), 0, 0));""",
        ).trim().toInt()
        val expected = CombatAdjustment.locationJumpChance(
            "The Spooky Forest", 0, 0, 0, 0,
        ) { MonsterDatabase.getByName(it) }
        assertEquals(expected, ash)
    }
}
