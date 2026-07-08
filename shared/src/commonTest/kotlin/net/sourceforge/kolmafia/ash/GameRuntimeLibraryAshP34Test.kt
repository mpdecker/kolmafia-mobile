package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP34Test {

    @Test
    fun monsterBracketId_readsBundledMonster() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("1341", outputLib(lib, """print(to_monster("huge mosquito")["id"]);""").trim())
        assertEquals("18", outputLib(lib, """print(to_monster("huge mosquito")["base_hp"]);""").trim())
        assertEquals("16", outputLib(lib, """print(to_monster("huge mosquito")["base_attack"]);""").trim())
        assertEquals("14", outputLib(lib, """print(to_monster("huge mosquito")["base_defense"]);""").trim())
        assertEquals("20", outputLib(lib, """print(to_monster("huge mosquito")["base_initiative"]);""").trim())
    }

    @Test
    fun monsterBracketPhylum_returnsPhylumType() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("bug", outputLib(lib, """print(to_monster("huge mosquito")["phylum"]);""").trim())
        assertEquals("false", outputLib(lib, """print(to_string(to_monster("huge mosquito")["boss"]));""").trim())
        assertEquals("giantmosquito.gif", outputLib(lib, """print(to_monster("huge mosquito")["image"]);""").trim())
        assertEquals("10", outputLib(lib, """print(to_monster("huge mosquito")["min_meat"]);""").trim())
        assertEquals("a", outputLib(lib, """print(to_monster("huge mosquito")["article"]);""").trim())
    }

    @Test
    fun monsterBracketUnknownMonster_returnsZeros() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("0", outputLib(lib, """print(to_monster("nonexistent critter")["id"]);""").trim())
        assertEquals("", outputLib(lib, """print(to_monster("nonexistent critter")["name"]);""").trim())
    }

    @Test
    fun monsterBracketCopyable_readsNocopyFlag() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "false",
            outputLib(
                lib,
                """print(to_string(to_monster("ancient protector spirit (The Hidden Apartment Building)")["copyable"]));""",
            ).trim(),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(to_monster("huge mosquito")["copyable"]));""").trim(),
        )
    }

    @Test
    fun monsterBracketUnknownField_throwsScriptException() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val failed = runCatching {
            outputLib(lib, """print(to_monster("huge mosquito")["manuel_name"]);""")
        }.isFailure
        assertTrue(failed)
    }

    @Test
    fun lastMonsterBracket_readsLastMonsterPref() = runBlocking {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(preferences = prefs, gameDatabase = db)
        assertEquals("giantmosquito.gif", outputLib(lib, """print(last_monster()["image"]);""").trim())
        assertEquals("10", outputLib(lib, """print(last_monster()["max_meat"]);""").trim())
    }
}
