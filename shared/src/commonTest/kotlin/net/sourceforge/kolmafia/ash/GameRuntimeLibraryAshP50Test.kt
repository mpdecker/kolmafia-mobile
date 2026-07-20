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

class GameRuntimeLibraryAshP50Test {

    @Test
    fun sourceAgent_attackEvaluatesPref() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "30",
            outputLib(lib, """print(monster_attack(to_monster("Source Agent")));""").trim(),
        )
        prefs.setString("sourceAgentsDefeated", "2")
        assertEquals(
            "90",
            outputLib(lib, """print(monster_attack(to_monster("Source Agent")));""").trim(),
        )
    }

    @Test
    fun shadowBat_attackEvaluatesPref() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("_shadowRiftCombats", "2")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        // Atk: [100+5*pref(_shadowRiftCombats)] → 110
        assertEquals(
            "110",
            outputLib(lib, """print(monster_attack(to_monster("shadow bat")));""").trim(),
        )
    }

    @Test
    fun baron_attackUsesBuffedMoxie() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(buffedmox = "10", buffedmus = "5", buffedmys = "5", ascensions = "0"),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        // Atk: [MOX+min(13,3+A)] with MOX=10, A=0 → 13
        assertEquals(
            "13",
            outputLib(lib, """print(monster_attack(to_monster("Baron von Ratsworth")));""").trim(),
        )
    }

    @Test
    fun expressionAtk_noOuterMlDoubleCount() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        // Bracket literal Atk: [200] — must stay 200 even if character ML is non-zero
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "200",
            outputLib(lib, """print(monster_attack(to_monster("drippy bat")));""").trim(),
        )
    }

    @Test
    fun numericAtk_unchanged() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "16",
            outputLib(lib, """print(monster_attack(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun revision_startsWithPhase() {
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
    }
}
