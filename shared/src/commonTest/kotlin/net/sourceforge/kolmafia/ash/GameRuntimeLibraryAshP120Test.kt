package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter

class GameRuntimeLibraryAshP120Test {

    @Test
    fun revision_phase170() {
        assertEquals("phase400", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun canEat_standardPath_true() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Standard"))
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("true", outputLib(lib, """print(can_eat());""").trim())
        assertEquals("15", outputLib(lib, """print(fullness_limit());""").trim())
    }

    @Test
    fun canEat_boozetafarian_false() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Boozetafarian"))
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("false", outputLib(lib, """print(can_eat());""").trim())
        assertEquals("0", outputLib(lib, """print(fullness_limit());""").trim())
    }

    @Test
    fun canDrink_teetotaler_false() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Teetotaler"))
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("false", outputLib(lib, """print(can_drink());""").trim())
        assertEquals("0", outputLib(lib, """print(inebriety_limit());""").trim())
    }

    @Test
    fun consumptionLimits_spelunkyLimitMode_blocked() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(path = "Standard", limitmode = "spelunky"),
            )
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("false", outputLib(lib, """print(can_eat());""").trim())
        assertEquals("false", outputLib(lib, """print(can_drink());""").trim())
        assertEquals("0", outputLib(lib, """print(fullness_limit());""").trim())
        assertEquals("0", outputLib(lib, """print(inebriety_limit());""").trim())
        assertEquals("0", outputLib(lib, """print(spleen_limit());""").trim())
    }

    @Test
    fun spleenLimit_standardPath_returnsSyncedLimit() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(path = "Standard", spleen = "3", spleensize = "15"),
            )
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("15", outputLib(lib, """print(spleen_limit());""").trim())
    }
}
