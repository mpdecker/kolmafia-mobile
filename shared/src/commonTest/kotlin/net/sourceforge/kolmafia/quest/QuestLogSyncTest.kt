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
    fun guildPacoVisit_appliesQuestHooksFromHtml() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = "<html>Welcome to Degrassi Knoll!</html>"
        val client = HttpClient(MockEngine { respond(html, HttpStatusCode.OK) })
        val lib = GameRuntimeLibrary(httpClient = client, questDatabase = db)
        runLib(lib, """cli_execute("guild paco");""")
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.MEATCAR))
    }

    @Test
    fun visitUrl_appliesQuestHooks() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = "<html>Welcome to Degrassi Knoll!</html>"
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(MockEngine { respond(html, HttpStatusCode.OK) }),
            questDatabase = db,
        )
        runLib(lib, """visit_url("guild.php?place=paco");""")
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.MEATCAR))
    }

    @Test
    fun applyPlaceHooks_nemesisStep8To9OnScgVisit() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step8")
        QuestLogSync.applyPlaceHooks("scg", db, QuestLogSync.QuestSyncContext())
        assertEquals("step9", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyPlaceHooks_nemesisStep16ToStep16_5OnScgVisit() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step16")
        QuestLogSync.applyPlaceHooks("scg", db, QuestLogSync.QuestSyncContext())
        assertEquals("step16.5", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyPlaceHooks_nemesisStep16_5To17OnScgVisit() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step16.5")
        QuestLogSync.applyPlaceHooks("scg", db, QuestLogSync.QuestSyncContext())
        assertEquals("step17", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyPlaceHooks_nemesisStep17_startsAssassinCounters() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.NEMESIS, "step16.5")
        val context = QuestLogSync.QuestSyncContext(preferences = prefs, currentRun = 42)
        QuestLogSync.applyPlaceHooks("scg", db, context)
        assertEquals("step17", db.getProgress(Quest.NEMESIS))
        assertTrue(prefs.getString("relayCounters", "").contains("Nemesis Assassin window begin"))
    }

    @Test
    fun applyPlaceHooks_factoryFinishedWhenEnvelopeInInventory() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.FACTORY, QuestDatabase.STARTED)
        val context = QuestLogSync.QuestSyncContext(
            hasItemId = { it == QuestLogSync.FACTORY_ENVELOPE_ID },
            place = "paco",
        )
        QuestLogSync.applyPlaceHooks("paco", db, context)
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.FACTORY))
    }

    @Test
    fun applyPlaceHooks_egoKeyTurnInAtGuild() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.EGO, QuestDatabase.STARTED)
        val context = QuestLogSync.QuestSyncContext(
            hasItemId = { it == QuestLogSync.FERNSWARTHY_KEY_ID },
            place = "ocg",
        )
        QuestLogSync.applyPlaceHooks("ocg", db, context)
        assertEquals("step1", db.getProgress(Quest.EGO))
    }

    @Test
    fun applyPlaceHooks_fernTowerUnlockWithKeyAtStep2() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.EGO, "step2")
        val context = QuestLogSync.QuestSyncContext(
            hasItemId = { it == QuestLogSync.FERNSWARTHY_KEY_ID },
            place = "fern",
        )
        QuestLogSync.applyPlaceHooks("fern", db, context)
        assertEquals("step3", db.getProgress(Quest.EGO))
    }

    @Test
    fun applyPlaceHooks_fernruinUnlocksTowerAtStep2() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.EGO, "step2")
        val context = QuestLogSync.QuestSyncContext(
            hasItemId = { it == QuestLogSync.FERNSWARTHY_KEY_ID },
        )
        QuestLogSync.applyPlaceHooks("fernruin", db, context)
        assertEquals("step3", db.getProgress(Quest.EGO))
    }

    @Test
    fun processResponse_setsFrCemeteryUnlockedOnSnarfblat507() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        kotlinx.coroutines.runBlocking {
            QuestLogSync.processResponse(
                "Visit place.php?snarfblat=507 for the cemetery.",
                db,
                null,
                QuestLogSync.QuestSyncContext(preferences = prefs),
            )
        }
        assertTrue(prefs.getBoolean(QuestLogSync.FR_CEMETERY_UNLOCKED_PREF, false))
    }

    @Test
    fun apply_nemesisStep10_excludesMettleFailureSignal() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step9")
        val text = "in the Big Mountains but you do not have not the required mettle to defeat"
        assertFalse(QuestAdvanceRules.apply(text, db))
        assertEquals("step9", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun apply_nemesisStep10_onBigMountainsSignal() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step9")
        assertTrue(QuestAdvanceRules.apply("You are in the Big Mountains now.", db))
        assertEquals("step10", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyDerivedQuestStatus_pyramidStartedFinishesDesert() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.DESERT, QuestDatabase.STARTED)
        db.setProgress(Quest.PYRAMID, QuestDatabase.STARTED)
        QuestLogSync.applyDerivedQuestStatus(db)
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.DESERT))
    }

    @Test
    fun applyDerivedQuestStatus_macguffinStep1FinishesBlack() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.BLACK, QuestDatabase.STARTED)
        db.setProgress(Quest.MACGUFFIN, "step1")
        QuestLogSync.applyDerivedQuestStatus(db)
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.BLACK))
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

    @Test
    fun applyDerivedQuestStatus_worshipStep3FinishesSideQuests() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.WORSHIP, "step4")
        db.setProgress(Quest.CURSES, QuestDatabase.STARTED)
        db.setProgress(Quest.DOCTOR, QuestDatabase.STARTED)
        QuestLogSync.applyDerivedQuestStatus(db)
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.CURSES))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.DOCTOR))
    }

    @Test
    fun applyDerivedQuestStatus_syncsWarAndPyramidPrefs() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PYRAMID, "step3")
        db.setProgress(Quest.ISLAND_WAR, "step1")
        QuestLogSync.applyDerivedQuestStatus(db, prefs, ascensionNumber = 5)
        assertTrue(prefs.getBoolean("middleChamberUnlock", false))
        assertTrue(prefs.getBoolean("lowerChamberUnlock", false))
        assertTrue(prefs.getBoolean("controlRoomUnlock", false))
        assertEquals("started", prefs.getString("warProgress", ""))
    }

    @Test
    fun applyDerivedQuestStatus_spookyravenChain() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.STARTED)
        db.setProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED)
        QuestLogSync.applyDerivedQuestStatus(db)
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.SPOOKYRAVEN_NECKLACE))
    }
}
