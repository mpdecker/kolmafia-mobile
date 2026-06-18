package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP25Test {

    private fun libWith(block: CharacterApiResponse.() -> CharacterApiResponse): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse().block())
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun statIsValid_acceptsCanonicalAndAliases() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_stat("Muscle"))));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_stat("submus"))));""").trim())
        assertEquals("false", outputLib(lib, """print(to_string(is_valid(to_stat("bogus"))));""").trim())
    }

    @Test
    fun statTypeOf_returnsStat() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("stat", outputLib(lib, """print(type_of(to_stat("Moxie")));""").trim())
    }

    @Test
    fun statNumericModifier_returnsZero() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "0.0",
            outputLib(lib, """print(to_string(numeric_modifier(to_stat("Muscle"), "Muscle")));""").trim(),
        )
    }

    @Test
    fun myBasestat_subMuscleReturnsSubpoints() = runBlocking {
        val lib = libWith { copy(musexp = "225") }
        assertEquals("225", outputLib(lib, """print(to_string(my_basestat(to_stat("SubMuscle"))));""").trim())
    }

    @Test
    fun myBuffedstat_subMuscleReturnsZero() = runBlocking {
        val lib = libWith { copy(musexp = "225") }
        assertEquals("0", outputLib(lib, """print(to_string(my_buffedstat(to_stat("SubMuscle"))));""").trim())
    }
}
