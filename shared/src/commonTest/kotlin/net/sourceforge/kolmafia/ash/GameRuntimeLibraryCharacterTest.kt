package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.adventure.AdventurePrep
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.AdventureZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
    fun canAdventure_overdrunkZoneRequiresInebriety() {
        val sober = net.sourceforge.kolmafia.character.CharacterState(adventuresLeft = 5, inebriety = 0)
        val zone = AdventureZone(
            zoneName = "Holiday",
            urlParams = "adventure=23",
            locationName = "Drunken Stupor",
            environment = "outdoor",
            diffLevel = "low",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = true,
            noWander = false,
        )
        assertFalse(AdventurePrep.canAdventureAt("Drunken Stupor", sober, zone))
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

    @Test
    fun resolveLocation_spookyForest_usesSnarfblat20() {
        val lib = GameRuntimeLibrary.forTesting()
        val loc = lib.resolveLocation("The Spooky Forest")
        assertEquals("20", loc?.id)
        assertEquals("The Spooky Forest", loc?.name)
    }

    @Test
    fun resolveLocation_unknownName_returnsNull() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals(null, lib.resolveLocation("Totally Fake Zone Name"))
    }

    @Test
    fun adv1_unknownLocation_returnsFalse() {
        val client = HttpClient(MockEngine { respond("") })
        val mgr = net.sourceforge.kolmafia.adventure.AdventureManager(
            adventureRequest = net.sourceforge.kolmafia.adventure.AdventureRequest(client),
            fightRequest = net.sourceforge.kolmafia.adventure.FightRequest(client),
            choiceRequest = net.sourceforge.kolmafia.adventure.ChoiceRequest(client),
            characterRequest = net.sourceforge.kolmafia.request.CharacterRequest(client),
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            preferences = prefs(),
            eventBus = net.sourceforge.kolmafia.event.GameEventBus(),
        )
        val lib = GameRuntimeLibrary(adventureManager = mgr)
        assertEquals("false", outputLib(lib, """print(to_string(adv1(to_location("Totally Fake Zone Name"), 1)));"""))
    }

    @Test
    fun turnsleft_matchesMyAdventures() {
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(adventures = "9")
        )
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("9", outputLib(lib, "print(to_string(turnsleft()));"))
        assertEquals("9", outputLib(lib, "print(to_string(my_adventures()));"))
    }

    @Test
    fun myAbsorbs_returnsCharacterAbsorbs() {
        val char = KoLCharacter()
        char.updateClassResource(absorbs = 4)
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("4", outputLib(lib, "print(to_string(my_absorbs()));"))
    }
}
