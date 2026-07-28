package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.canonicalElementOrder
import net.sourceforge.kolmafia.data.primaryAttackElement

class GameRuntimeLibraryAshP64Test {

    @Test
    fun parse_multiEa_haxxor() = runBlocking {
        MonsterDatabase.load()
        val haxxor = MonsterDatabase.getByName("1335 haxx0r")!!
        assertEquals(listOf("none", "bad spelling"), canonicalElementOrder(haxxor.attackElements))
        assertEquals("bad spelling", primaryAttackElement(haxxor.attackElements))
        assertEquals("bad spelling", haxxor.attackElement)
    }

    @Test
    fun parse_multiEa_amcGremlin() = runBlocking {
        MonsterDatabase.load()
        val gremlin = MonsterDatabase.getByName("a.m.c. gremlin")!!
        assertEquals(listOf("none", "hot"), canonicalElementOrder(gremlin.attackElements))
        assertEquals("hot", primaryAttackElement(gremlin.attackElements))
    }

    @Test
    fun parse_multiEa_axeWound() = runBlocking {
        MonsterDatabase.load()
        val axe = MonsterDatabase.getByName("axe wound")!!
        assertEquals(listOf("none", "cold", "hot"), canonicalElementOrder(axe.attackElements))
        assertEquals("hot", primaryAttackElement(axe.attackElements))
    }

    @Test
    fun attackElements_mosquitoEmpty() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(emptyList(), mosquito.attackElements)
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(lib, """print(count(to_monster("huge mosquito")["attack_elements"]));""").trim(),
        )
        assertEquals(
            "",
            outputLib(lib, """print(to_monster("huge mosquito")["attack_element"]);""").trim(),
        )
    }

    @Test
    fun attackElements_apsSingleSpooky() = runBlocking {
        MonsterDatabase.load()
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "1",
            outputLib(
                lib,
                """print(count(to_monster("ancient protector spirit")["attack_elements"]));""",
            ).trim(),
        )
        assertEquals(
            "spooky",
            outputLib(
                lib,
                """print(to_monster("ancient protector spirit")["attack_elements"][0]);""",
            ).trim(),
        )
        assertEquals(
            "spooky",
            outputLib(
                lib,
                """print(to_monster("ancient protector spirit")["attack_element"]);""",
            ).trim(),
        )
    }

    @Test
    fun attackElements_amcGremlin_twoElements() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "2",
            outputLib(
                lib,
                """print(count(to_monster("A.M.C. gremlin")["attack_elements"]));""",
            ).trim(),
        )
        assertEquals(
            "none",
            outputLib(
                lib,
                """print(to_monster("A.M.C. gremlin")["attack_elements"][0]);""",
            ).trim(),
        )
        assertEquals(
            "hot",
            outputLib(
                lib,
                """print(to_monster("A.M.C. gremlin")["attack_elements"][1]);""",
            ).trim(),
        )
        assertEquals(
            "hot",
            outputLib(lib, """print(to_monster("A.M.C. gremlin")["attack_element"]);""").trim(),
        )
    }

    @Test
    fun attackElements_haxxor_badSpelling() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "2",
            outputLib(
                lib,
                """print(count(to_monster("1335 HaXx0r")["attack_elements"]));""",
            ).trim(),
        )
        assertEquals(
            "none",
            outputLib(
                lib,
                """print(to_monster("1335 HaXx0r")["attack_elements"][0]);""",
            ).trim(),
        )
        assertEquals(
            "bad spelling",
            outputLib(
                lib,
                """print(to_monster("1335 HaXx0r")["attack_elements"][1]);""",
            ).trim(),
        )
        assertEquals(
            "bad spelling",
            outputLib(lib, """print(to_monster("1335 HaXx0r")["attack_element"]);""").trim(),
        )
    }

    @Test
    fun attackElements_axeWound_enumOrderLastHot() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "3",
            outputLib(lib, """print(count(to_monster("Axe Wound")["attack_elements"]));""").trim(),
        )
        assertEquals(
            "none",
            outputLib(lib, """print(to_monster("Axe Wound")["attack_elements"][0]);""").trim(),
        )
        assertEquals(
            "cold",
            outputLib(lib, """print(to_monster("Axe Wound")["attack_elements"][1]);""").trim(),
        )
        assertEquals(
            "hot",
            outputLib(lib, """print(to_monster("Axe Wound")["attack_elements"][2]);""").trim(),
        )
        assertEquals(
            "hot",
            outputLib(lib, """print(to_monster("Axe Wound")["attack_element"]);""").trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase181", GameRuntimeLibrary.REVISION)
    }
}
