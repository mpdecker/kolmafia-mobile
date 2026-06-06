package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
        val client = HttpClient(engine) {
            install(HttpCookies)
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
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

        manager.runAdventures(testLocation, 1, this)
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

        manager.runAdventures(testLocation, 5, this)
            .join()

        collectJob.cancel()
        val stopped = received.filterIsInstance<GameEvent.AdventureLoopStopped>()
        assertEquals(1, stopped.size)
        assertIs<StopReason.NoAdventuresLeft>(stopped.first().reason)
    }

    @Test
    fun recoveryLoop_exitsAfterFirstIter_whenNoItemsOrSkillsAvailable() = runTest {
        // Build a mock engine: adventure.php returns a non-combat result, api.php returns
        // a status with low HP (30/100) so needsHpRecovery fires on the first iteration.
        // No items or skills are in the response, so recoverHp/recoverMp return false
        // immediately and the loop exits after 0 heal actions.
        val lowHpStatus = """{"name":"Player","playerid":"1","level":"5","class":"1","hp":"30","hpmax":"100","mp":"30","mpmax":"50","meat":"1000","adventures":"40","fullness":"0","drunk":"0","spleen":"0"}"""
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.contains("adventure.php") ->
                    respond(NON_COMBAT_HTML, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"))
                request.url.encodedPath.contains("api.php") ->
                    respond(lowHpStatus, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) { install(HttpCookies) }
        val bus = GameEventBus()
        val character = KoLCharacter()
        val prefs = Preferences(MapSettings())
        // AUTO_RECOVER_HP defaults to true; no items or skills available
        val invManager = InventoryManager(client, bus)
        val skillManager = SkillManager(client, SkillCastRequest(client), bus)
        val recoveryMgr = RecoveryManager(invManager, skillManager, prefs)

        val received = mutableListOf<GameEvent>()
        val manager = AdventureManager(
            AdventureRequest(client),
            FightRequest(client),
            ChoiceRequest(client),
            CharacterRequest(client),
            character,
            prefs,
            bus,
            recoveryManager = recoveryMgr,
        )

        val collectJob = launch { bus.events.collect { received.add(it) } }
        manager.runAdventures(testLocation, 1, this).join()
        collectJob.cancel()

        // The loop must complete without error and still emit TurnConsumed
        val turns = received.filterIsInstance<GameEvent.TurnConsumed>()
        assertEquals(1, turns.size, "Expected exactly one TurnConsumed event")
        assertIs<AdventureResult.NonCombat>(turns.first().result)
    }

    @Test
    fun runAdventures_stopsWithGoalMet_whenItemGoalSatisfied() = runTest {
        val (manager, bus, received) = makeManager(adventureHtml = NON_COMBAT_WITH_ITEM_HTML)
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.goalManager.addItemGoalByName("rat whisker")
        manager.runAdventures(testLocation, 10, this).join()

        collectJob.cancel()
        val stopped = received.filterIsInstance<GameEvent.AdventureLoopStopped>()
        assertEquals(1, stopped.size)
        assertIs<StopReason.GoalMet>(stopped.first().reason)
        // TurnConsumed emitted before GoalMet, so exactly 1 turn consumed
        assertEquals(1, received.filterIsInstance<GameEvent.TurnConsumed>().size)
    }

    @Test
    fun runAdventures_doesNotStopForGoal_whenNoItemGoalSet() = runTest {
        val (manager, bus, received) = makeManager(adventureHtml = NON_COMBAT_WITH_ITEM_HTML)
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.runAdventures(testLocation, 3, this).join()

        collectJob.cancel()
        assertFalse(
            received.filterIsInstance<GameEvent.AdventureLoopStopped>()
                .any { it.reason is StopReason.GoalMet }
        )
        assertEquals(3, received.filterIsInstance<GameEvent.TurnConsumed>().size)
    }

    @Test
    fun runAdventures_doesNotStop_whenItemNameDoesNotMatchGoal() = runTest {
        val (manager, bus, received) = makeManager(adventureHtml = NON_COMBAT_WITH_ITEM_HTML)
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.goalManager.addItemGoalByName("completely different item")
        manager.runAdventures(testLocation, 2, this).join()

        collectJob.cancel()
        assertFalse(
            received.filterIsInstance<GameEvent.AdventureLoopStopped>()
                .any { it.reason is StopReason.GoalMet }
        )
        assertEquals(2, received.filterIsInstance<GameEvent.TurnConsumed>().size)
    }

    @Test
    fun runAdventures_stopsWithCharacterDeath_notGoalMet_whenDieWithGoalItem() = runTest {
        // Fight that drops a goal item but the player loses
        val (manager, bus, received) = makeManager(
            adventureHtml = COMBAT_HTML,
            fightHtml = COMBAT_LOSS_WITH_ITEM_HTML,
        )
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.goalManager.addItemGoalByName("rat whisker")
        manager.runAdventures(testLocation, 5, this).join()

        collectJob.cancel()
        val stopped = received.filterIsInstance<GameEvent.AdventureLoopStopped>()
        // Only ONE stop event — CharacterDeath, not GoalMet
        assertEquals(1, stopped.size)
        assertIs<StopReason.CharacterDeath>(stopped.first().reason)
    }

    @Test
    fun runAdventures_stopsWithGoalMet_whenMeatGoalReached() = runTest {
        val (manager, bus, received) = makeManager(statusJson = STATUS_JSON_HIGH_MEAT)
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.goalManager.setMeatGoal(40_000)  // 50_000 >= 40_000 → stop
        manager.runAdventures(testLocation, 5, this).join()

        collectJob.cancel()
        val stopped = received.filterIsInstance<GameEvent.AdventureLoopStopped>()
        assertEquals(1, stopped.size)
        val reason = stopped.first().reason
        assertIs<StopReason.GoalMet>(reason)
        assertTrue((reason as StopReason.GoalMet).description.contains("meat", ignoreCase = true))
        assertEquals(1, received.filterIsInstance<GameEvent.TurnConsumed>().size)
    }

    @Test
    fun runAdventures_stopsWithGoalMet_whenLevelGoalReached() = runTest {
        val (manager, bus, received) = makeManager(statusJson = STATUS_JSON_HIGH_LEVEL)
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.goalManager.setLevelGoal(12)  // level 15 >= 12 → stop
        manager.runAdventures(testLocation, 5, this).join()

        collectJob.cancel()
        val stopped = received.filterIsInstance<GameEvent.AdventureLoopStopped>()
        assertEquals(1, stopped.size)
        assertIs<StopReason.GoalMet>(stopped.first().reason)
        assertEquals(1, received.filterIsInstance<GameEvent.TurnConsumed>().size)
    }

    @Test
    fun runAdventures_doesNotStop_whenMeatBelowGoal() = runTest {
        // STATUS_JSON_ADVENTURES_LEFT has meat=1000
        val (manager, bus, received) = makeManager()
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.goalManager.setMeatGoal(999_999)  // far above meat=1000
        manager.runAdventures(testLocation, 2, this).join()

        collectJob.cancel()
        assertFalse(
            received.filterIsInstance<GameEvent.AdventureLoopStopped>()
                .any { it.reason is StopReason.GoalMet }
        )
        assertEquals(2, received.filterIsInstance<GameEvent.TurnConsumed>().size)
    }

    @Test
    fun runAdventures_doesNotStop_whenLevelBelowGoal() = runTest {
        // STATUS_JSON_ADVENTURES_LEFT has level=5
        val (manager, bus, received) = makeManager()
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.goalManager.setLevelGoal(20)  // far above level=5
        manager.runAdventures(testLocation, 2, this).join()

        collectJob.cancel()
        assertFalse(
            received.filterIsInstance<GameEvent.AdventureLoopStopped>()
                .any { it.reason is StopReason.GoalMet }
        )
        assertEquals(2, received.filterIsInstance<GameEvent.TurnConsumed>().size)
    }

    companion object {
        const val NON_COMBAT_HTML = """<html><body><b>A Spooky Treehouse</b><p>You gain 10 Meat.</p></body></html>"""
        const val COMBAT_WIN_HTML = """<html><body><span id='monname'>bunny</span><p>You win the fight!</p></body></html>"""
        const val STATUS_JSON_ADVENTURES_LEFT = """{"name":"Player","playerid":"1","level":"5","class":"1","hp":"50","hpmax":"100","mp":"30","mpmax":"50","meat":"1000","adventures":"40","fullness":"0","drunk":"0","spleen":"0"}"""
        const val STATUS_JSON_NO_ADVENTURES = """{"name":"Player","playerid":"1","level":"5","class":"1","hp":"50","hpmax":"100","mp":"30","mpmax":"50","meat":"1000","adventures":"0","fullness":"0","drunk":"0","spleen":"0"}"""
        const val STATUS_JSON_HIGH_MEAT = """{"name":"Player","playerid":"1","level":"5","class":"1","hp":"50","hpmax":"100","mp":"30","mpmax":"50","meat":"50000","adventures":"40","fullness":"0","drunk":"0","spleen":"0"}"""
        const val STATUS_JSON_HIGH_LEVEL = """{"name":"Player","playerid":"1","level":"15","class":"1","hp":"50","hpmax":"100","mp":"30","mpmax":"50","meat":"1000","adventures":"40","fullness":"0","drunk":"0","spleen":"0"}"""
        const val NON_COMBAT_WITH_ITEM_HTML = """<html><body><b>A Spooky Treehouse</b>
<p>You acquire an item: <b>rat whisker</b></p>
<p>You gain 10 Meat.</p></body></html>"""
        // HTML that triggers combat routing (contains "You're fighting")
        const val COMBAT_HTML = """<html><body>You're fighting a monster!</body></html>"""
        // Fight result: player loses but drops an item
        const val COMBAT_LOSS_WITH_ITEM_HTML = """<html><body><span id='monname'>Knob Goblin</span>
<p>You acquire an item: <b>rat whisker</b></p>
<p>You lose the fight.</p></body></html>"""
    }
}
