package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP52Test {

    @Test
    fun sourceAgent_hpEvaluatesPref() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "40",
            outputLib(lib, """print(monster_hp(to_monster("Source Agent")));""").trim(),
        )
        prefs.setString("sourceAgentsDefeated", "2")
        assertEquals(
            "120",
            outputLib(lib, """print(monster_hp(to_monster("Source Agent")));""").trim(),
        )
    }

    @Test
    fun baron_hpUsesCharacterMaxHp() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(hpmax = "100"))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        // HP: [HP*1.25] with character maxHp=100 → 125
        assertEquals(
            "125",
            outputLib(lib, """print(monster_hp(to_monster("Baron von Ratsworth")));""").trim(),
        )
    }

    @Test
    fun expressionHp_noOuterMlDoubleCount() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        // Bracket literal HP: [120]
        assertEquals(
            "120",
            outputLib(lib, """print(monster_hp(to_monster("drippy bat")));""").trim(),
        )
    }

    @Test
    fun numericHp_unchanged() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "18",
            outputLib(lib, """print(monster_hp(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun revision_isPhase94() {
        assertEquals("phase290", GameRuntimeLibrary.REVISION)
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
    }
}
