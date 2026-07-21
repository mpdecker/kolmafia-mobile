package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP42Test {

    @Test
    fun monsterElement_oneArg_matchesDefenseElement() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "sleaze",
            outputLib(lib, """print(monster_element(to_monster("caveman frat boy")));""").trim(),
        )
        assertEquals(
            "sleaze",
            outputLib(lib, """print(monster_element(to_monster("Axe Wound")));""").trim(),
        )
        assertEquals(
            "",
            outputLib(lib, """print(monster_element(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun monsterElement_zeroArg_readsLastMonster() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "caveman frat boy")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals("sleaze", outputLib(lib, """print(monster_element());""").trim())
    }

    @Test
    fun monsterElement_brackets_exposeAttackAndDefense() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "hot",
            outputLib(lib, """print(to_monster("Axe Wound")["attack_element"]);""").trim(),
        )
        assertEquals(
            "sleaze",
            outputLib(lib, """print(to_monster("Axe Wound")["defense_element"]);""").trim(),
        )
        assertEquals(
            "spooky",
            outputLib(
                lib,
                """print(to_monster("ancient protector spirit")["attack_element"]);""",
            ).trim(),
        )
        assertEquals(
            "",
            outputLib(
                lib,
                """print(to_monster("ancient protector spirit")["defense_element"]);""",
            ).trim(),
        )
    }
}
