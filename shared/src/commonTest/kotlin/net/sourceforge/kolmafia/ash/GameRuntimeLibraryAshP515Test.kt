package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.FightRequest
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest

class GameRuntimeLibraryAshP515Test {

    private class RecordingAdventureManager : AdventureManager(
        adventureRequest = AdventureRequest(HttpClient(MockEngine { respond("") })),
        fightRequest = FightRequest(HttpClient(MockEngine { respond("") })),
        choiceRequest = ChoiceRequest(HttpClient(MockEngine { respond("") })),
        characterRequest = CharacterRequest(HttpClient(MockEngine { respond("") })),
        character = KoLCharacter(),
        preferences = prefs(),
        eventBus = GameEventBus(),
    ) {
        val calls = mutableListOf<Pair<String, Int>>()

        override fun runAdventures(location: AdventureLocation, turns: Int, scope: CoroutineScope): Job {
            calls += location.name to turns
            return Job().apply { complete() }
        }
    }

    @Test
    fun revision_phase515() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun adventure_unknownLocation_printsError() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("adventure zzznosuchlocation999");""")
        assertTrue(out.contains("does not exist in the adventure database."))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun adventureCheckOnly_printsLocationWithoutRunning() {
        val mgr = RecordingAdventureManager()
        val out = outputLib(
            GameRuntimeLibrary(adventureManager = mgr),
            """cli_execute("adventure? The Haunted Pantry");""",
        )
        assertTrue(out.contains("The Haunted Pantry"))
        assertEquals(emptyList(), mgr.calls)
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun adventure_recordsTurnCount() {
        val mgr = RecordingAdventureManager()
        val out = outputLib(
            GameRuntimeLibrary(adventureManager = mgr),
            """cli_execute("adventure 2 The Haunted Pantry");""",
        )
        assertEquals(listOf("The Haunted Pantry" to 2), mgr.calls)
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun adventureLast_usesLastAdventurePref() {
        val mgr = RecordingAdventureManager()
        val p = prefs()
        p.setString("lastAdventure", "The Haunted Pantry")
        val out = outputLib(
            GameRuntimeLibrary(adventureManager = mgr, preferences = p),
            """cli_execute("adventure last");""",
        )
        assertEquals(listOf("The Haunted Pantry" to 1), mgr.calls)
        assertFalse(out.contains("[cli]"))
    }
}
