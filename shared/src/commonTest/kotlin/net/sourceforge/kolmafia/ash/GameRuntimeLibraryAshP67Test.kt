package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP67Test {

    @Test
    fun factType_mosquitoDefaultCharacter() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "effect",
            outputLib(lib, """print(to_monster("huge mosquito")["fact_type"]);""").trim(),
        )
        assertEquals(
            "Disabled Olfactory Processing (10)",
            outputLib(lib, """print(to_monster("huge mosquito")["fact"]);""").trim(),
        )
    }

    @Test
    fun factType_mosquitoSealClubberUsesBugPool() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(classId = "1"))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "effect",
            outputLib(lib, """print(to_monster("huge mosquito")["fact_type"]);""").trim(),
        )
        assertEquals(
            "Industrial Strength Starch (15)",
            outputLib(lib, """print(to_monster("huge mosquito")["fact"]);""").trim(),
        )
    }

    @Test
    fun fact_unknownMonsterEmpty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "",
            outputLib(lib, """print(to_monster("nonexistent critter")["fact"]);""").trim(),
        )
        assertEquals(
            "",
            outputLib(lib, """print(to_monster("nonexistent critter")["fact_type"]);""").trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase479", GameRuntimeLibrary.REVISION)
    }
}
