package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter

class GameRuntimeLibraryAshP430Test {

    private fun libFromApi(response: CharacterApiResponse): GameRuntimeLibrary {
        val char = KoLCharacter().also { it.updateFromApiResponse(response) }
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun revision_phase480() {
        assertEquals("phase485", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun limit_mode_returnsCanonicalName() {
        val lib = libFromApi(CharacterApiResponse(limitmode = "spelunk"))
        assertEquals("spelunky", outputLib(lib, """print(limit_mode());""").trim())
    }

    @Test
    fun in_casual_readsApiFlag() {
        val lib = libFromApi(CharacterApiResponse(casual = "1"))
        assertEquals("true", outputLib(lib, """print(in_casual());""").trim())
    }

    @Test
    fun turns_played_and_my_turncount_readCurrentRun() {
        val lib = libFromApi(CharacterApiResponse(currentrun = "42"))
        assertEquals("42", outputLib(lib, """print(turns_played());""").trim())
        assertEquals("42", outputLib(lib, """print(my_turncount());""").trim())
    }

    @Test
    fun total_turns_played_readsAllTimeTurns() {
        val lib = libFromApi(CharacterApiResponse(turnsplayed = "99999"))
        assertEquals("99999", outputLib(lib, """print(total_turns_played());""").trim())
    }

    @Test
    fun daycount_readsGlobalDaynumber() {
        val lib = libFromApi(CharacterApiResponse(daynumber = "12345"))
        assertEquals("12345", outputLib(lib, """print(daycount());""").trim())
    }
}
