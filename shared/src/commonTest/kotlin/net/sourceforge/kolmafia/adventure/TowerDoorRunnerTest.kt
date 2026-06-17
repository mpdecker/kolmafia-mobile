package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class TowerDoorRunnerTest {

    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    @Test
    fun runInternal_wrongQuestStep_abortsWithMessage() = runTest {
        val testPrefs = prefs { setString(Quest.FINAL.prefKey, "step4") }
        val db = QuestDatabase(testPrefs)
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val messages = mutableListOf<String>()
        val runner = TowerDoorRunner(
            httpClient = client,
            character = KoLCharacter(),
            preferences = testPrefs,
            questDatabase = db,
            inventoryManager = null,
            retrieveItemService = null,
            gameDatabase = null,
            skillCastRequest = null,
            choiceRequest = null,
            coinmasterManager = null,
            skillManager = null,
            processQuestHooks = { _, _ -> },
        )
        assertFalse(runner.runInternal { messages.add(it) })
        assertTrue(messages.any { it.contains("haven't reached the Tower Door") })
    }

    @Test
    fun runInternal_unlocksAndTurnsDoorknobToStep6() = runTest {
        val testPrefs = prefs { setString(Quest.FINAL.prefKey, "step5") }
        val db = QuestDatabase(testPrefs)
        val keyIds = towerKeyIds()
        val invManager = stubInventoryManager(keyIds)
        val client = HttpClient(MockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("action=ns_doorknob") ->
                    respond("<html>You turn the knob and the door vanishes.</html>", HttpStatusCode.OK)
                url.contains("action=ns_lock") ->
                    respond("<html>the lock vanishes</html>", HttpStatusCode.OK)
                else ->
                    respond(
                        """<html>
                        <a href="ns_lock1 ">1</a>
                        <a href="ns_lock2 ">2</a>
                        <a href="ns_lock3 ">3</a>
                        <a href="ns_lock4 ">4</a>
                        <a href="ns_lock5 ">5</a>
                        <a href="ns_lock6 ">6</a>
                        <a href="ns_doorknob ">k</a>
                        </html>""",
                        HttpStatusCode.OK,
                    )
            }
        })
        val runner = TowerDoorRunner(
            httpClient = client,
            character = KoLCharacter(),
            preferences = testPrefs,
            questDatabase = db,
            inventoryManager = invManager,
            retrieveItemService = null,
            gameDatabase = null,
            skillCastRequest = null,
            choiceRequest = null,
            coinmasterManager = null,
            skillManager = null,
            processQuestHooks = { html, url ->
                TowerDoorConfig.extractDoorAction(url)?.let { action ->
                    net.sourceforge.kolmafia.quest.TowerSync.parseTowerDoorResponse(
                        action,
                        html,
                        testPrefs,
                        db,
                    )
                } ?: if (url.contains(TowerDoorConfig.DOOR_PLACE) ||
                    url.contains(TowerDoorConfig.LOW_KEY_DOOR_PLACE)
                ) {
                    TowerDoorConfig.syncTowerDoorFromHtml(html, testPrefs, TowerDoorConfig.STANDARD_LOCKS)
                } else {
                    Unit
                }
            },
        )
        val messages = mutableListOf<String>()
        assertTrue(runner.runInternal { messages.add(it) })
        assertTrue(messages.any { it.contains("Tower Door open!") })
        assertEquals("step6", db.getProgress(Quest.FINAL))
    }

    @Test
    fun runInternal_lowKeyAdventureKey_abortsWithGuidance() = runTest {
        val testPrefs = prefs { setString(Quest.FINAL.prefKey, "step5") }
        val db = QuestDatabase(testPrefs)
        val char = KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(path = "Low Key"),
        )
        val client = HttpClient(MockEngine { respond(
            """<html><a href="nstower_doowlowkey1 ">lock</a></html>""",
            HttpStatusCode.OK,
        ) })
        val messages = mutableListOf<String>()
        val runner = TowerDoorRunner(
            httpClient = client,
            character = char,
            preferences = testPrefs,
            questDatabase = db,
            inventoryManager = null,
            retrieveItemService = null,
            gameDatabase = null,
            skillCastRequest = null,
            choiceRequest = null,
            coinmasterManager = null,
            skillManager = null,
            processQuestHooks = { html, url ->
                TowerDoorConfig.extractDoorAction(url)?.let { action ->
                    net.sourceforge.kolmafia.quest.TowerSync.parseTowerDoorResponse(
                        action,
                        html,
                        testPrefs,
                        db,
                        char.state.value,
                    )
                } ?: if (url.contains(TowerDoorConfig.LOW_KEY_DOOR_PLACE)) {
                    TowerDoorConfig.syncTowerDoorFromHtml(
                        html,
                        testPrefs,
                        TowerDoorConfig.LOW_KEY_LOCKS,
                    )
                } else {
                    Unit
                }
            },
        )
        assertFalse(runner.runInternal { messages.add(it) })
        assertTrue(messages.any { it.contains("Adventure in") && it.contains("Fun") })
    }

    @Test
    fun runInternal_lowKeyUnlocksWithLkActions() = runTest {
        val testPrefs = prefs { setString(Quest.FINAL.prefKey, "step5") }
        val db = QuestDatabase(testPrefs)
        val char = KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(path = "Low Key"),
        )
        val keyIds = towerKeyIds()
        val invManager = stubInventoryManager(keyIds)
        val client = HttpClient(MockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("action=ns_doorknob_lk") ->
                    respond("<html>You turn the knob and the door vanishes.</html>", HttpStatusCode.OK)
                url.contains("action=ns_lock") ->
                    respond("<html>the lock vanishes</html>", HttpStatusCode.OK)
                else ->
                    respond(
                        """<html>
                        <a href="ns_lock1_lk ">1</a>
                        <a href="ns_lock2_lk ">2</a>
                        <a href="ns_lock3_lk ">3</a>
                        <a href="ns_lock4_lk ">4</a>
                        <a href="ns_lock5_lk ">5</a>
                        <a href="ns_lock6_lk ">6</a>
                        <a href="ns_doorknob_lk ">k</a>
                        </html>""",
                        HttpStatusCode.OK,
                    )
            }
        })
        val runner = TowerDoorRunner(
            httpClient = client,
            character = char,
            preferences = testPrefs,
            questDatabase = db,
            inventoryManager = invManager,
            retrieveItemService = null,
            gameDatabase = null,
            skillCastRequest = null,
            choiceRequest = null,
            coinmasterManager = null,
            skillManager = null,
            processQuestHooks = { html, url ->
                TowerDoorConfig.extractDoorAction(url)?.let { action ->
                    net.sourceforge.kolmafia.quest.TowerSync.parseTowerDoorResponse(
                        action,
                        html,
                        testPrefs,
                        db,
                        char.state.value,
                    )
                } ?: if (url.contains(TowerDoorConfig.LOW_KEY_DOOR_PLACE)) {
                    TowerDoorConfig.syncTowerDoorFromHtml(
                        html,
                        testPrefs,
                        TowerDoorConfig.LOW_KEY_LOCKS,
                    )
                } else {
                    Unit
                }
            },
        )
        val messages = mutableListOf<String>()
        assertTrue(runner.runInternal { messages.add(it) })
        assertEquals("step6", db.getProgress(Quest.FINAL))
    }

    private fun towerKeyIds() = listOf(
        ItemPool.BORIS_KEY,
        ItemPool.JARLSBERG_KEY,
        ItemPool.SNEAKY_PETE_KEY,
        ItemPool.STAR_KEY,
        ItemPool.DIGITAL_KEY,
        ItemPool.SKELETON_KEY,
    )

    private fun stubInventoryManager(keyIds: List<Int>): InventoryManager {
        val items = keyIds.associateWith { id ->
            InventoryItem(id, "key$id", 1, ItemType.OTHER)
        }
        return object : InventoryManager(
            client = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
            eventBus = net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            init {
                _state.value = InventoryState(items = items)
            }
        }
    }
}
