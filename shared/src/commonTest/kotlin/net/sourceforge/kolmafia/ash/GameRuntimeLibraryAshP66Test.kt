package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP66Test {

    @Test
    fun parse_attributes_mosquito() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(
            "Atk: 16 Def: 14 HP: 18 Init: 20 Meat: 10 P: bug Article: a",
            mosquito.attributes,
        )
        assertEquals(emptyList(), mosquito.randomModifiers)
    }

    @Test
    fun parse_attributes_apsContainsGhostAndEa() = runBlocking {
        MonsterDatabase.load()
        val aps = MonsterDatabase.getByName("ancient protector spirit")!!
        assertTrue(aps.attributes.contains("GHOST"))
        assertTrue(aps.attributes.contains("EA: spooky"))
    }

    @Test
    fun attributes_mosquitoBracket() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "Atk: 16 Def: 14 HP: 18 Init: 20 Meat: 10 P: bug Article: a",
            outputLib(lib, """print(to_monster("huge mosquito")["attributes"]);""").trim(),
        )
    }

    @Test
    fun randomModifiers_mosquitoEmpty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(
                lib,
                """print(count(to_monster("huge mosquito")["random_modifiers"]));""",
            ).trim(),
        )
    }

    @Test
    fun randomModifiers_apsEmpty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(
                lib,
                """print(count(to_monster("ancient protector spirit")["random_modifiers"]));""",
            ).trim(),
        )
    }

    @Test
    fun attributes_unknownMonsterEmpty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "",
            outputLib(lib, """print(to_monster("nonexistent critter")["attributes"]);""").trim(),
        )
        assertEquals(
            "0",
            outputLib(
                lib,
                """print(count(to_monster("nonexistent critter")["random_modifiers"]));""",
            ).trim(),
        )
    }

    @Test
    fun randomModifiers_lastMonsterEmptyWithoutTracker() = runBlocking {
        MonsterStatusTracker.resetLastMonster()
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(com.russhwolf.settings.MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "0",
            outputLib(
                lib,
                """print(count(last_monster()["random_modifiers"]));""",
            ).trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase160", GameRuntimeLibrary.REVISION)
    }
}
