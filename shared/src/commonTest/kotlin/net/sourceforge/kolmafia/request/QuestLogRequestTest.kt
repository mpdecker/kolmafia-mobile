package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.QuestLogDatabase
import net.sourceforge.kolmafia.data.QuestLogConsequenceDatabase
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
        val request = QuestLogRequest(client, questDb, Preferences(settings))
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

    @Test fun parsePage_page1_clearsAbsentCompletedQuestPrefs() = runTest {
        val settings = MapSettings()
        val prefs = Preferences(settings)
        prefs.setString("ghostLocation", "The Spooky Forest")
        prefs.setString("_newYouQuestMonster", "ghost")
        prefs.setString("doctorBagQuestItem", "scalpel")
        prefs.setString("doctorBagQuestLocation", "Distant Woods")
        val questDb = db(settings)
        QuestLogDatabase.injectForTest(emptyList())
        val client = HttpClient(MockEngine { _ -> respond("<html><body></body></html>", HttpStatusCode.OK) })
        val request = QuestLogRequest(client, questDb, prefs)
        request.parsePage("<html><body></body></html>", 1)
        assertEquals("", prefs.getString("ghostLocation", "x"))
        assertEquals("", prefs.getString("_newYouQuestMonster", "x"))
        assertEquals("", prefs.getString("doctorBagQuestItem", "x"))
        assertEquals("", prefs.getString("doctorBagQuestLocation", "x"))
        assertEquals(0, prefs.getInt("_newYouQuestSharpensDone", -1))
        assertEquals(QuestDatabase.FINISHED, questDb.getProgress(Quest.TOOT))
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

    @Test fun parsePage_partyFairStep2_notClobberedByUnparseableBody() {
        val settings = MapSettings()
        val prefs = Preferences(settings)
        prefs.setString("_questPartyFairQuest", "partiers")
        prefs.setString("_questPartyFairProgress", "3")
        val questDb = db(settings)
        questDb.setProgress(Quest.PARTY_FAIR, "step2")
        QuestLogDatabase.injectForTest(listOf(
            QuestLogEntry(
                prefKey = "_questPartyFair",
                title = "Party Fair",
                steps = listOf(
                    "started" to "start the party",
                    "step1" to "clean up trash",
                    "step2" to "return to the party",
                    "finished" to "party over",
                ),
            ),
        ))
        val request = QuestLogRequest(
            HttpClient(MockEngine { respond("ok") }),
            questDb,
            prefs,
        )
        request.parsePage(
            "<html><body><b>Party Fair</b><br>Some vague party status text.</body></html>",
            1,
        )
        assertEquals("step2", questDb.getProgress(Quest.PARTY_FAIR))
    }

    @Test fun parsePage_batStep2_notRegressedByQuestLogStep1() {
        val settings = MapSettings()
        val questDb = db(settings)
        questDb.setProgress(Quest.BAT, "step2")
        QuestLogDatabase.injectForTest(listOf(
            QuestLogEntry(
                prefKey = Quest.BAT.prefKey,
                title = "Ooh, I Think I Smell a Bat.",
                steps = listOf(
                    "started" to "find and defeat the boss bat",
                    "step1" to "continue searching for the boss bat",
                    "step2" to "(no unique message)",
                    "step3" to "defeat the boss bat",
                    "finished" to "you have slain the boss bat",
                ),
            ),
        ))
        val request = QuestLogRequest(
            HttpClient(MockEngine { respond("ok") }),
            questDb,
        )
        request.parsePage(
            "<html><body><b>Ooh, I Think I Smell a Bat.</b><br>Continue searching for the Boss Bat.</body></html>",
            1,
        )
        assertEquals("step2", questDb.getProgress(Quest.BAT))
    }

    @Test fun parsePage_page3_setsDemonNameFromAccomplishments() {
        QuestLogConsequenceDatabase.injectForTest(
            QuestLogConsequenceDatabase.parseForTest(
                "QUEST_LOG	demonName9	;&middot;([^<]*?), the Smith<br	demonName9=\$1",
            ),
        )
        val settings = MapSettings()
        val prefs = Preferences(settings)
        val request = QuestLogRequest(
            HttpClient(MockEngine { respond("ok") }),
            db(settings),
            prefs,
        )
        request.parsePage(";&middot;Hammer Time, the Smith<br", which = 3)
        assertEquals("Hammer Time", prefs.getString("demonName9", ""))
    }
}
