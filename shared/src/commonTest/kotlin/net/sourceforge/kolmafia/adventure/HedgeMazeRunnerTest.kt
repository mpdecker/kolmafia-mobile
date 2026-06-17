package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.ash.HedgeMazeMode
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.CharacterRequest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HedgeMazeRunnerTest {

    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    private fun stubManager(client: HttpClient, prefs: Preferences): AdventureManager =
        AdventureManager(
            adventureRequest = AdventureRequest(client),
            fightRequest = FightRequest(client),
            choiceRequest = ChoiceRequest(client),
            characterRequest = CharacterRequest(client),
            character = KoLCharacter(),
            preferences = prefs,
            eventBus = GameEventBus(),
            questDatabase = QuestDatabase(prefs),
        )

    @Test
    fun runInternal_insufficientAdventures_abortsWithMessage() = runTest {
        val testPrefs = prefs {
            setString(Quest.FINAL.prefKey, "step4")
            setInt("choiceAdventure1005", 2)
            setInt("choiceAdventure1006", 1)
            setInt("choiceAdventure1007", 1)
            setInt("choiceAdventure1008", 2)
            setInt("choiceAdventure1009", 1)
            setInt("choiceAdventure1010", 1)
            setInt("choiceAdventure1011", 2)
            setInt("choiceAdventure1012", 1)
            setInt("choiceAdventure1013", 1)
        }
        val db = QuestDatabase(testPrefs)
        val char = KoLCharacter()
        char.updateAdventuresLeft(2)
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val messages = mutableListOf<String>()
        val runner = HedgeMazeRunner(
            httpClient = client,
            adventureManager = stubManager(client, testPrefs),
            character = char,
            preferences = testPrefs,
            questDatabase = db,
            recoveryManager = null,
            inventoryManager = null,
            skillManager = null,
            effectManager = null,
            processQuestHooks = { _, _ -> },
        )
        assertFalse(runner.runInternal(HedgeMazeMode.TRAPS) { messages.add(it) })
        assertTrue(messages.any { it.contains("more adventure") })
    }

    @Test
    fun runInternal_completesWhenQuestAdvancesToStep5() = runTest {
        val testPrefs = prefs {
            setString(Quest.FINAL.prefKey, "step4")
            for (room in 1005..1013) setInt("choiceAdventure$room", 1)
        }
        val db = QuestDatabase(testPrefs)
        val char = KoLCharacter()
        char.updateAdventuresLeft(20)
        var visits = 0
        val client = HttpClient(MockEngine {
            visits++
            respond("<html><p>You wander deeper into the hedge maze.</p></html>", HttpStatusCode.OK)
        })
        val runner = HedgeMazeRunner(
            httpClient = client,
            adventureManager = stubManager(client, testPrefs),
            character = char,
            preferences = testPrefs,
            questDatabase = db,
            recoveryManager = null,
            inventoryManager = null,
            skillManager = null,
            effectManager = null,
            processQuestHooks = { _, _ ->
                if (visits >= 1) db.setProgress(Quest.FINAL, "step5")
            },
        )
        val messages = mutableListOf<String>()
        assertTrue(runner.runInternal(HedgeMazeMode.NUGGLETS) { messages.add(it) })
        assertTrue(messages.any { it.contains("Hedge Maze cleared!") })
    }
}
