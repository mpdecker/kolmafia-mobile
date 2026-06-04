package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.QuestLogRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class AdventureManagerQuestTest {

    private var syncCallCount = 0

    private fun countingSyncRequest(): QuestLogRequest {
        val client = HttpClient(MockEngine { _ ->
            respond("<html></html>", HttpStatusCode.OK)
        })
        val db = QuestDatabase(Preferences(MapSettings()))
        return object : QuestLogRequest(client, db) {
            override suspend fun syncAll() { syncCallCount++ }
        }
    }

    private fun minimalManager(questLogRequest: QuestLogRequest?): AdventureManager {
        val client = HttpClient(MockEngine { _ -> respond("", HttpStatusCode.OK) })
        val prefs  = Preferences(MapSettings())
        val bus    = GameEventBus()
        val char   = KoLCharacter()
        return AdventureManager(
            adventureRequest = AdventureRequest(client),
            fightRequest     = FightRequest(client),
            choiceRequest    = ChoiceRequest(client),
            characterRequest = CharacterRequest(client),
            character        = char,
            preferences      = prefs,
            eventBus         = bus,
            questLogRequest  = questLogRequest,
        )
    }

    @Test fun checkQuestAdvancement_signalPresent_callsSyncAll() = runTest {
        syncCallCount = 0
        val manager = minimalManager(countingSyncRequest())
        manager.testCheckQuestAdvancement("Your quest log has been updated.")
        assertEquals(1, syncCallCount)
    }

    @Test fun checkQuestAdvancement_noSignal_doesNotCallSyncAll() = runTest {
        syncCallCount = 0
        val manager = minimalManager(countingSyncRequest())
        manager.testCheckQuestAdvancement("You fought a monster and won.")
        assertEquals(0, syncCallCount)
    }

    @Test fun checkQuestAdvancement_caseInsensitive_callsSyncAll() = runTest {
        syncCallCount = 0
        val manager = minimalManager(countingSyncRequest())
        manager.testCheckQuestAdvancement("QUEST COMPLETED! Well done.")
        assertEquals(1, syncCallCount)
    }

    @Test fun checkQuestAdvancement_nullQuestLogRequest_doesNotCrash() = runTest {
        val manager = minimalManager(null)
        manager.testCheckQuestAdvancement("Quest Completed")  // must not throw
    }
}
