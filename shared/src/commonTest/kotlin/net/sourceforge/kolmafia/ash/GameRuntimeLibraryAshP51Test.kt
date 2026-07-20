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

class GameRuntimeLibraryAshP51Test {

    @Test
    fun sourceAgent_defenseEvaluatesPref() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "30",
            outputLib(lib, """print(monster_defense(to_monster("Source Agent")));""").trim(),
        )
        prefs.setString("sourceAgentsDefeated", "2")
        assertEquals(
            "90",
            outputLib(lib, """print(monster_defense(to_monster("Source Agent")));""").trim(),
        )
    }

    @Test
    fun baron_defenseUsesBuffedMuscle() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(buffedmox = "5", buffedmus = "10", buffedmys = "5", ascensions = "0"),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        // Def: [MUS+min(13,3+A)] with MUS=10, A=0 → 13
        assertEquals(
            "13",
            outputLib(lib, """print(monster_defense(to_monster("Baron von Ratsworth")));""").trim(),
        )
    }

    @Test
    fun expressionDef_noOuterMlDoubleCount() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        // Bracket literal Def: [200]
        assertEquals(
            "200",
            outputLib(lib, """print(monster_defense(to_monster("drippy bat")));""").trim(),
        )
    }

    @Test
    fun numericDef_unchanged() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "14",
            outputLib(lib, """print(monster_defense(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun revision_startsWithPhase() {
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
    }
}
