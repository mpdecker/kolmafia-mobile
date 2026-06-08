package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryCharacterTest {

    private fun libWith(block: CharacterApiResponse.() -> CharacterApiResponse): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse().block())
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun myClass_sealClubber() {
        val lib = libWith { copy(classId = "1") }
        assertEquals("Seal Clubber", outputLib(lib, "print(my_class());"))
    }

    @Test
    fun inRun_falseWhenKingLiberated() {
        val lib = libWith { copy(kingliberated = "1") }
        assertEquals("false", outputLib(lib, "print(to_string(in_run()));"))
    }

    @Test
    fun inRun_trueWhenActive() {
        val lib = libWith { copy(kingliberated = "0") }
        assertEquals("true", outputLib(lib, "print(to_string(in_run()));"))
    }

    @Test
    fun canInteract_falseInHardcore() {
        val lib = libWith { copy(hardcore = "1") }
        assertEquals("false", outputLib(lib, "print(to_string(can_interact()));"))
    }

    @Test
    fun canInteract_falseInRonin() {
        val lib = libWith { copy(roninleft = "5") }
        assertEquals("false", outputLib(lib, "print(to_string(can_interact()));"))
    }

    @Test
    fun mySign_returnsZodiacSign() {
        val lib = libWith { copy(sign = "Opossum") }
        assertEquals("Opossum", outputLib(lib, "print(my_sign());"))
    }

    @Test
    fun underStandard_trueWhenPathIsStandard() {
        val lib = libWith { copy(path = "Standard") }
        assertEquals("true", outputLib(lib, "print(to_string(under_standard()));"))
    }

    @Test
    fun underStandard_falseWhenPathIsNone() {
        val lib = libWith { copy(path = "None") }
        assertEquals("false", outputLib(lib, "print(to_string(under_standard()));"))
    }

    @Test
    fun underStandard_falseWhenNoCharacter() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, "print(to_string(under_standard()));"))
    }

    @Test
    fun ascensionNumber_returns42() {
        val lib = libWith { copy(ascensions = "42") }
        assertEquals("42", outputLib(lib, "print(to_string(ascension_number()));"))
    }

    @Test
    fun myThrall_emptyString() {
        assertEquals("", outputLib(GameRuntimeLibrary.forTesting(), "print(my_thrall());"))
    }

    @Test
    fun canAdventure_trueWhenAdventuresLeft() {
        val lib = libWith { copy(adventures = "5") }
        assertEquals("true", outputLib(lib, """print(to_string(can_adventure(to_location("The Haunted Pantry"))));"""))
    }

    @Test
    fun canAdventure_falseWhenNoAdventuresLeft() {
        val lib = libWith { copy(adventures = "0") }
        assertEquals("false", outputLib(lib, """print(to_string(can_adventure(to_location("The Haunted Pantry"))));"""))
    }

    @Test
    fun prepareForAdventure_returnsTrue() {
        assertEquals("true", outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(prepare_for_adventure()));"))
    }

    @Test
    fun adv1_returnsFalseWhenNoAdventureManager() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, """print(to_string(adv1(to_location("The Haunted Pantry"), 1)));"""))
    }
}
