package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.PoisonLevels

class GameRuntimeLibraryAshP63Test {

    @Test
    fun poison_swarmSomewhatPoisoned() = runBlocking {
        MonsterDatabase.load()
        val bees = MonsterDatabase.getByName("swarm of killer bees")!!
        assertEquals(4, bees.poison)
        assertEquals("Somewhat Poisoned", PoisonLevels.effectNameForLevel(bees.poison))
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "Somewhat Poisoned",
            outputLib(lib, """print(to_monster("swarm of killer bees")["poison"]);""").trim(),
        )
    }

    @Test
    fun poison_mosquitoNone() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(Int.MAX_VALUE, mosquito.poison)
        assertEquals("none", PoisonLevels.effectNameForLevel(mosquito.poison))
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "none",
            outputLib(lib, """print(to_monster("huge mosquito")["poison"]);""").trim(),
        )
    }

    @Test
    fun group_swarmSix_defaultOne() = runBlocking {
        MonsterDatabase.load()
        val bees = MonsterDatabase.getByName("swarm of killer bees")!!
        assertEquals(6, bees.group)
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(1, mosquito.group)
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "6",
            outputLib(lib, """print(to_monster("swarm of killer bees")["group"]);""").trim(),
        )
        assertEquals(
            "1",
            outputLib(lib, """print(to_monster("huge mosquito")["group"]);""").trim(),
        )
    }

    @Test
    fun manuel_apartmentApsOverride() = runBlocking {
        MonsterDatabase.load()
        val aps = MonsterDatabase.getByName(
            "ancient protector spirit (The Hidden Apartment Building)",
        )!!
        assertEquals("ancient protector spirit", aps.manuelName)
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(null, mosquito.manuelName)
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "ancient protector spirit",
            outputLib(
                lib,
                """print(to_monster("ancient protector spirit (The Hidden Apartment Building)")["manuel_name"]);""",
            ).trim(),
        )
        assertEquals(
            "huge mosquito",
            outputLib(lib, """print(to_monster("huge mosquito")["manuel_name"]);""").trim(),
        )
    }

    @Test
    fun wiki_mimicOverride() = runBlocking {
        MonsterDatabase.load()
        val mimic = MonsterDatabase.getByName("mimic")!!
        assertEquals("mimic (Cloak)", mimic.wikiName)
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(null, mosquito.wikiName)
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "mimic (Cloak)",
            outputLib(lib, """print(to_monster("mimic")["wiki_name"]);""").trim(),
        )
        assertEquals(
            "huge mosquito",
            outputLib(lib, """print(to_monster("huge mosquito")["wiki_name"]);""").trim(),
        )
    }

    @Test
    fun revision_phase111() {
        assertEquals("phase111", GameRuntimeLibrary.REVISION)
    }
}
