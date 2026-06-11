package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.runLib
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.QuestLogRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestLogSyncTest {

    @Test
    fun shouldSync_matchesQuestLogPhrases() {
        assertTrue(QuestLogSync.shouldSync("Your quest log has been updated."))
        assertTrue(QuestLogSync.shouldSync("Thanks for the larva"))
        assertFalse(QuestLogSync.shouldSync("You fight a seal."))
    }

    @Test
    fun processResponse_appliesRulesAndSyncs() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        var synced = false
        val questLog = object : QuestLogRequest(HttpClient(MockEngine { respond("") }), db) {
            override suspend fun syncAll() {
                synced = true
            }
        }
        kotlinx.coroutines.runBlocking {
            QuestLogSync.processResponse(
                "Well done!  You have slain the Boss Bat.",
                db,
                questLog,
            )
        }
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.BAT))
        assertTrue(synced)
    }

    @Test
    fun processResponse_noSyncWhenNoSignal() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        var synced = false
        val questLog = object : QuestLogRequest(HttpClient(MockEngine { respond("") }), db) {
            override suspend fun syncAll() {
                synced = true
            }
        }
        kotlinx.coroutines.runBlocking {
            QuestLogSync.processResponse("You acquire an item: seal tooth", db, questLog)
        }
        assertFalse(synced)
    }

    @Test
    fun councilVisit_appliesQuestHooksFromHtml() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = "<html>You must slay the Boss Bat in the Bat Hole.</html>"
        val client = HttpClient(MockEngine { respond(html, HttpStatusCode.OK) })
        val lib = GameRuntimeLibrary(
            httpClient = client,
            questDatabase = db,
        )
        runLib(lib, """cli_execute("council");""")
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.BAT))
    }
}
