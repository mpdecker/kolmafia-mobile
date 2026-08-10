package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.FightRequest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.request.CharacterRequest

class GameRuntimeLibraryAshP248Test {

    @Test
    fun revision_phase236() {
        assertEquals("phase400", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun dailyUsesLeft_zeroDuringMultiFight() = runTest {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    fullness = "0",
                    stomachsize = "15",
                ),
            )
        }
        val mgr = stubAdventureManager()
        mgr.testSetCombatFlags(inMultiFight = true, fightFollowsChoice = false)
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            character = char,
            preferences = prefs(),
            adventureManager = mgr,
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_item("hot wing")["dailyusesleft"]);""").trim(),
        )
    }

    @Test
    fun dailyUsesLeft_zeroDuringChoiceFollowsFight() = runTest {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    fullness = "0",
                    stomachsize = "15",
                ),
            )
        }
        val mgr = stubAdventureManager()
        mgr.testSetCombatFlags(inMultiFight = true, fightFollowsChoice = true)
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            character = char,
            preferences = prefs(),
            adventureManager = mgr,
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_item("hot wing")["dailyusesleft"]);""").trim(),
        )
    }

    private fun stubAdventureManager() = AdventureManager(
        adventureRequest = AdventureRequest(HttpClient(MockEngine { respond("") })),
        fightRequest = FightRequest(HttpClient(MockEngine { respond("") })),
        choiceRequest = ChoiceRequest(HttpClient(MockEngine { respond("") })),
        characterRequest = CharacterRequest(HttpClient(MockEngine { respond("") })),
        character = KoLCharacter(),
        preferences = prefs(),
        eventBus = GameEventBus(),
    )
}
