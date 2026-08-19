package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase

class GameRuntimeLibraryAshP72Test {

    @Test
    fun toMonster_beefyBat_beecoreBoostsBaseAttack() = runBlocking {
        MonsterDatabase.load()
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(path = "Bees Hate You", kingliberated = "0"),
        )
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "40",
            outputLib(lib, """print(to_monster("beefy bodyguard bat")["base_attack"]);""").trim(),
        )
        assertEquals(
            "25",
            outputLib(lib, """print(to_monster("beefy bodyguard bat")["raw_attack"]);""").trim(),
        )
    }

    @Test
    fun toMonster_beefyBat_nonBeecoreUnchanged() = runBlocking {
        MonsterDatabase.load()
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "25",
            outputLib(lib, """print(to_monster("beefy bodyguard bat")["base_attack"]);""").trim(),
        )
        assertEquals(
            "25",
            outputLib(lib, """print(to_monster("beefy bodyguard bat")["raw_attack"]);""").trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }
}
