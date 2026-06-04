package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.QuestLogDatabase
import net.sourceforge.kolmafia.data.QuestLogEntry
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class QuestLogRequestTest {

    private fun db(settings: MapSettings = MapSettings()) =
        QuestDatabase(Preferences(settings))

    private val fixtureHtmlPage1 = """
        <html><body>
        <b>Looking for a Larva in All the Wrong Places</b><br>
        Return to the Council of Loathing with the mosquito larva.
        <p>
        <b>Ooh, I Think I Smell a Rat</b><br>
        Go talk to the owner of the Typical Tavern in the Distant Woods.
        </body></html>
    """.trimIndent()

    @Test fun parsePage_knownQuest_setsProgress() = runTest {
        val settings = MapSettings()
        val questDb = db(settings)
        QuestLogDatabase.injectForTest(listOf(
            QuestLogEntry(
                prefKey = "questL02Larva",
                title   = "Looking for a Larva in All the Wrong Places",
                steps   = listOf(
                    "started"  to "go find the larva",
                    "step1"    to "return to the council of loathing with the mosquito larva",
                    "finished" to "you delivered a mosquito larva"
                )
            )
        ))
        val client = HttpClient(MockEngine { _ -> respond(fixtureHtmlPage1, HttpStatusCode.OK) })
        val request = QuestLogRequest(client, questDb)
        request.syncPage(1)
        assertEquals("step1", questDb.getProgress(Quest.LARVA))
    }

    @Test fun parsePage_unknownQuest_skipped() = runTest {
        val questDb = db()
        QuestLogDatabase.injectForTest(emptyList())  // no entries
        val client = HttpClient(MockEngine { _ -> respond(fixtureHtmlPage1, HttpStatusCode.OK) })
        val request = QuestLogRequest(client, questDb)
        request.syncPage(1)
        // Unknown titles → skipped; LARVA stays at default
        assertEquals(QuestDatabase.UNSTARTED, questDb.getProgress(Quest.LARVA))
    }

    @Test fun parsePage_httpError_doesNotCrash() = runTest {
        val questDb = db()
        val client = HttpClient(MockEngine { _ -> respond("", HttpStatusCode.InternalServerError) })
        val request = QuestLogRequest(client, questDb)
        request.syncPage(1)  // must not throw
    }

    @Test fun syncAll_fetchesThreePages() = runTest {
        var callCount = 0
        val client = HttpClient(MockEngine { _ ->
            callCount++
            respond("<html></html>", HttpStatusCode.OK)
        })
        QuestLogDatabase.injectForTest(emptyList())
        val request = QuestLogRequest(client, db())
        request.syncAll()
        assertEquals(3, callCount)
    }
}
