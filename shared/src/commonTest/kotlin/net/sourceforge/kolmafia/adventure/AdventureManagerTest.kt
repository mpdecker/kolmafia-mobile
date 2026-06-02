package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AdventureManagerTest {

    private val testLocation = AdventureLocation("17", "Spooky Forest", "Nearby Plains")

    private fun makeManager(
        adventureHtml: String = NON_COMBAT_HTML,
        fightHtml: String = COMBAT_WIN_HTML,
        statusJson: String = STATUS_JSON_ADVENTURES_LEFT
    ): Triple<AdventureManager, GameEventBus, MutableList<GameEvent>> {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.contains("adventure.php") ->
                    respond(adventureHtml, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"))
                request.url.encodedPath.contains("fight.php") ->
                    respond(fightHtml, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"))
                request.url.encodedPath.contains("choice.php") ->
                    respond(NON_COMBAT_HTML, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"))
                request.url.encodedPath.contains("api.php") ->
                    respond(statusJson, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) { install(HttpCookies) }
        val character = KoLCharacter()
        val prefs = Preferences(MapSettings())
        val bus = GameEventBus()
        val received = mutableListOf<GameEvent>()

        val manager = AdventureManager(
            AdventureRequest(client),
            FightRequest(client),
            ChoiceRequest(client),
            CharacterRequest(client),
            character,
            prefs,
            bus
        )
        return Triple(manager, bus, received)
    }

    @Test
    fun runAdventures_emitsTurnConsumed_forNonCombat() = runTest {
        val (manager, bus, received) = makeManager()
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.runAdventures(testLocation, 1, CoroutineScope(Dispatchers.Default))
            .join()

        collectJob.cancel()
        val turns = received.filterIsInstance<GameEvent.TurnConsumed>()
        assertEquals(1, turns.size)
        assertIs<AdventureResult.NonCombat>(turns.first().result)
    }

    @Test
    fun runAdventures_stopsWith_noAdventuresLeft() = runTest {
        val (manager, bus, received) = makeManager(
            statusJson = STATUS_JSON_NO_ADVENTURES
        )
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.runAdventures(testLocation, 5, CoroutineScope(Dispatchers.Default))
            .join()

        collectJob.cancel()
        val stopped = received.filterIsInstance<GameEvent.AdventureLoopStopped>()
        assertEquals(1, stopped.size)
        assertIs<StopReason.NoAdventuresLeft>(stopped.first().reason)
    }

    companion object {
        const val NON_COMBAT_HTML = """<html><body><b>A Spooky Treehouse</b><p>You gain 10 Meat.</p></body></html>"""
        const val COMBAT_WIN_HTML = """<html><body><span id='monname'>bunny</span><p>You win the fight!</p></body></html>"""
        const val STATUS_JSON_ADVENTURES_LEFT = """{"name":"Player","playerid":"1","level":"5","class":"1","hp":"50","hpmax":"100","mp":"30","mpmax":"50","meat":"1000","adventures":"40","fullness":"0","drunk":"0","spleen":"0"}"""
        const val STATUS_JSON_NO_ADVENTURES = """{"name":"Player","playerid":"1","level":"5","class":"1","hp":"50","hpmax":"100","mp":"30","mpmax":"50","meat":"1000","adventures":"0","fullness":"0","drunk":"0","spleen":"0"}"""
    }
}
