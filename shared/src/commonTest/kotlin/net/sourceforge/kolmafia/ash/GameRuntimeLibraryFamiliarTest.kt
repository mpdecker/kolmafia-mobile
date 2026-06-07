package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryFamiliarTest {

    private val goat = FamiliarData(
        id = 7, name = "Biscuit", race = "Angry Goat",
        weight = 12, experience = 0, kills = 0
    )

    private fun makeFamiliarManager(state: FamiliarState = FamiliarState()): FamiliarManager {
        val fm = FamiliarManager(HttpClient(MockEngine { respond("") }), GameEventBus())
        fm.testSetState(state)
        return fm
    }

    private fun libWithGoat(): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.updateFamiliar(id = 7, name = "Biscuit", weight = 12, exp = 0)
        val fm = makeFamiliarManager(FamiliarState(ownedFamiliars = listOf(goat)))
        return GameRuntimeLibrary(character = char, familiarManager = fm)
    }

    @Test
    fun haveFamiliar_trueWhenOwned() {
        assertEquals("true",
            outputLib(libWithGoat(),
                """print(to_string(have_familiar(to_familiar("Angry Goat"))));"""))
    }

    @Test
    fun haveFamiliar_falseWhenNotOwned() {
        val lib = GameRuntimeLibrary(familiarManager = makeFamiliarManager())
        assertEquals("false",
            outputLib(lib,
                """print(to_string(have_familiar(to_familiar("Purse Rat"))));"""))
    }

    @Test
    fun toFamiliar_roundTripsName() {
        assertEquals("Angry Goat",
            outputLib(GameRuntimeLibrary.forTesting(),
                """print(to_familiar("Angry Goat"));"""))
    }

    @Test
    fun myFamiliarWeight_returnsFromCharacterState() {
        val char = KoLCharacter()
        char.updateFamiliar(id = 7, name = "Biscuit", weight = 12, exp = 0)
        val lib = GameRuntimeLibrary(
            character = char,
            familiarManager = makeFamiliarManager(FamiliarState(ownedFamiliars = listOf(goat)))
        )
        assertEquals("12", outputLib(lib, "print(to_string(my_familiar_weight()));"))
    }

    @Test
    fun useFamiliar_returnsTrueWhenOwned() {
        // libWithGoat() has the goat registered and mock returns success
        assertEquals("true",
            outputLib(libWithGoat(),
                """print(to_string(use_familiar(to_familiar("Angry Goat"))));"""))
    }

    @Test
    fun useFamiliar_returnsFalseWithNoManager() {
        // no familiarManager injected → returns false immediately
        assertEquals("false",
            outputLib(GameRuntimeLibrary.forTesting(),
                """print(to_string(use_familiar(to_familiar("Angry Goat"))));"""))
    }
}
