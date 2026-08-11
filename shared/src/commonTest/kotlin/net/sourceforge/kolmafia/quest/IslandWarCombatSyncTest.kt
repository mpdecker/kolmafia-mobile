package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandWarCombatSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun sessionLogger(prefs: Preferences): SessionLogger =
        SessionLogger(prefs, GameEventBus())

    private fun sessionLog(prefs: Preferences): String =
        prefs.getString(SessionLogger.SESSION_LOG_KEY, "")

    private fun winResponse(extra: String = ""): String =
        "WINWINWIN $extra".trim()

    private companion object {
        private const val FRAT_BATTLEFIELD = "132"
        private const val HIPPY_BATTLEFIELD = "140"
        private const val WAR_HIPPY_MONSTER = "War Hippy Infantryman"
        private const val WAR_FRAT_MONSTER = "War Frat Wartender"
    }

    @BeforeTest
    fun loadDatabases() = runBlocking {
        GameDatabase().load()
    }

    @Test
    fun applyCombatWin_lossOnBattlefield132_noIncrement() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        prefs.setInt("hippiesDefeated", 10)
        assertFalse(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse(),
                won = false,
                monster = WAR_HIPPY_MONSTER,
            ),
        )
        assertEquals(10, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOn132WithoutMessage_incrementsByOne() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse(),
                won = true,
                monster = WAR_HIPPY_MONSTER,
            ),
        )
        assertEquals(1, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOn132WithTier2Message_incrementsByFour() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse("You hose down three hippies with sauce."),
                won = true,
                monster = WAR_HIPPY_MONSTER,
            ),
        )
        assertEquals(4, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOn132WithTier1Message_incrementsByTwo() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse("He runs away protesting the war."),
                won = true,
                monster = WAR_HIPPY_MONSTER,
            ),
        )
        assertEquals(2, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOn140WithRocketLauncher_incrementsByFour() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = HIPPY_BATTLEFIELD,
                responseText = winResponse("His rocket launcher blasts 3 extra frat boys."),
                won = true,
                monster = WAR_FRAT_MONSTER,
            ),
        )
        assertEquals(4, prefs.getInt("fratboysDefeated", 0))
    }

    @Test
    fun applyCombatWin_winWhenWarFinished_noIncrement() {
        val prefs = prefs()
        prefs.setString("warProgress", "finished")
        prefs.setInt("hippiesDefeated", 10)
        assertFalse(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse(),
                won = true,
                monster = WAR_HIPPY_MONSTER,
            ),
        )
        assertEquals(10, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOnNonBattlefield_noIncrement() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertFalse(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = "20",
                responseText = winResponse(),
                won = true,
                monster = "spooky gravy fairy guard",
            ),
        )
        assertEquals(0, prefs.getInt("hippiesDefeated", 0))
        assertEquals(0, prefs.getInt("fratboysDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOnNonBattlefield_warMonsterStillIncrements() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = "20",
                responseText = winResponse(),
                won = true,
                monster = WAR_HIPPY_MONSTER,
            ),
        )
        assertEquals(1, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOn132AtKoeCap_staysAtCap() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        prefs.setInt("hippiesDefeated", 333)
        assertFalse(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse(),
                won = true,
                monster = WAR_HIPPY_MONSTER,
                isKingdomOfExploathing = true,
            ),
        )
        assertEquals(333, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_unknownMonster_logsAndSkips() {
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        prefs.setString("warProgress", "started")
        prefs.setInt("hippiesDefeated", 5)
        assertFalse(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse(),
                won = true,
                monster = "totally fake monster",
                sessionLogger = logger,
            ),
        )
        assertEquals(5, prefs.getInt("hippiesDefeated", 0))
        assertTrue(sessionLog(prefs).contains("Unknown monster found on battlefield: totally fake monster"))
    }

    @Test
    fun applyCombatWin_unexpectedMonster_logsAndSkips() {
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        prefs.setString("warProgress", "started")
        prefs.setInt("hippiesDefeated", 5)
        assertFalse(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse(),
                won = true,
                monster = "spooky gravy fairy guard",
                sessionLogger = logger,
            ),
        )
        assertEquals(5, prefs.getInt("hippiesDefeated", 0))
        assertTrue(sessionLog(prefs).contains("Unexpected monster found on battlefield: spooky gravy fairy guard"))
    }

    @Test
    fun applyCombatWin_validHippyMonster_increments() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse(),
                won = true,
                monster = "War Hippy Baker",
            ),
        )
        assertEquals(1, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_validFratMonster_increments() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = HIPPY_BATTLEFIELD,
                responseText = winResponse(),
                won = true,
                monster = WAR_FRAT_MONSTER,
            ),
        )
        assertEquals(1, prefs.getInt("fratboysDefeated", 0))
    }

    private fun questDb(prefs: Preferences = prefs()): QuestDatabase = QuestDatabase(prefs)

    @Test
    fun applyEndOfWar_bigWisniewskiOn132_finishesIslandWar() {
        val prefs = prefs()
        val db = questDb(prefs)
        prefs.setString("warProgress", "started")
        prefs.setInt("hippiesDefeated", 50)
        assertTrue(
            IslandWarCombatSync.applyEndOfWar(
                questDatabase = db,
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                monster = "The Big Wisniewski",
                responseText = winResponse(),
                won = true,
            ),
        )
        assertEquals(1000, prefs.getInt("hippiesDefeated", 0))
        assertEquals("hippies", prefs.getString("sideDefeated", ""))
        assertEquals("finished", prefs.getString("warProgress", ""))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.ISLAND_WAR))
    }

    @Test
    fun applyEndOfWar_theManOn140KoE_finishesHippyFrat() {
        val prefs = prefs()
        val db = questDb(prefs)
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyEndOfWar(
                questDatabase = db,
                preferences = prefs,
                adventureId = HIPPY_BATTLEFIELD,
                monster = "The Man",
                responseText = winResponse(),
                won = true,
                isKingdomOfExploathing = true,
            ),
        )
        assertEquals(333, prefs.getInt("fratboysDefeated", 0))
        assertEquals("fratboys", prefs.getString("sideDefeated", ""))
        assertEquals("finished", prefs.getString("warProgress", ""))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.HIPPY_FRAT))
    }

    @Test
    fun applyEndOfWar_giantExplosions_finishesBothSidesAt1000() {
        val prefs = prefs()
        val db = questDb(prefs)
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyEndOfWar(
                questDatabase = db,
                preferences = prefs,
                adventureId = "999",
                monster = WAR_FRAT_MONSTER,
                responseText = winResponse("Giant explosions in slow motion"),
                won = true,
                isKingdomOfExploathing = true,
            ),
        )
        assertEquals(1000, prefs.getInt("fratboysDefeated", 0))
        assertEquals(1000, prefs.getInt("hippiesDefeated", 0))
        assertEquals("both", prefs.getString("sideDefeated", ""))
        assertEquals("finished", prefs.getString("warProgress", ""))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.HIPPY_FRAT))
    }

    @Test
    fun applyEndOfWar_whenWarFinished_noChanges() {
        val prefs = prefs()
        val db = questDb(prefs)
        prefs.setString("warProgress", "finished")
        prefs.setInt("hippiesDefeated", 1000)
        db.setProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED)
        assertFalse(
            IslandWarCombatSync.applyEndOfWar(
                questDatabase = db,
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                monster = "The Big Wisniewski",
                responseText = winResponse(),
                won = true,
            ),
        )
        assertEquals(1000, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyEndOfWar_bossOnNonBattlefield_noEndOfWar() {
        val prefs = prefs()
        val db = questDb(prefs)
        prefs.setString("warProgress", "started")
        assertFalse(
            IslandWarCombatSync.applyEndOfWar(
                questDatabase = db,
                preferences = prefs,
                adventureId = "20",
                monster = "The Big Wisniewski",
                responseText = winResponse(),
                won = true,
            ),
        )
        assertEquals("started", prefs.getString("warProgress", ""))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.ISLAND_WAR))
    }

    @Test
    fun applyEndOfWar_bossWinSkipsCounterIncrement() {
        val prefs = prefs()
        val db = questDb(prefs)
        prefs.setString("warProgress", "started")
        prefs.setInt("hippiesDefeated", 10)
        assertTrue(
            IslandWarCombatSync.applyEndOfWar(
                questDatabase = db,
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                monster = "The Big Wisniewski",
                responseText = winResponse("He runs away protesting the war."),
                won = true,
            ),
        )
        assertEquals(1000, prefs.getInt("hippiesDefeated", 0))
        assertFalse(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse("He runs away protesting the war."),
                won = true,
                monster = "The Big Wisniewski",
            ),
        )
    }

    @Test
    fun applyNunsSidequestWin_hospitalPhrase_setsHippy() {
        val prefs = prefs()
        prefs.setString("sidequestNunsCompleted", "none")
        assertTrue(
            IslandWarCombatSync.applyNunsSidequestWin(
                preferences = prefs,
                monster = "dirty thieving brigand",
                responseText = "could serve as a hospital for our wounded troops",
                won = true,
            ),
        )
        assertEquals("hippy", prefs.getString("sidequestNunsCompleted", ""))
    }

    @Test
    fun applyNunsSidequestWin_massageParlorPhrase_setsFratboy() {
        val prefs = prefs()
        prefs.setString("sidequestNunsCompleted", "none")
        assertTrue(
            IslandWarCombatSync.applyNunsSidequestWin(
                preferences = prefs,
                monster = "dirty thieving brigand",
                responseText = "could serve as a massage parlor for our troops",
                won = true,
            ),
        )
        assertEquals("fratboy", prefs.getString("sidequestNunsCompleted", ""))
    }

    @Test
    fun applyNunsSidequestWin_wrongMonster_noChange() {
        val prefs = prefs()
        prefs.setString("sidequestNunsCompleted", "none")
        assertFalse(
            IslandWarCombatSync.applyNunsSidequestWin(
                preferences = prefs,
                monster = WAR_FRAT_MONSTER,
                responseText = "could serve as a hospital",
                won = true,
            ),
        )
        assertEquals("none", prefs.getString("sidequestNunsCompleted", ""))
    }

    @Test
    fun applyNunsSidequestWin_loss_noChange() {
        val prefs = prefs()
        prefs.setString("sidequestNunsCompleted", "none")
        assertFalse(
            IslandWarCombatSync.applyNunsSidequestWin(
                preferences = prefs,
                monster = "dirty thieving brigand",
                responseText = "could serve as a hospital",
                won = false,
            ),
        )
        assertEquals("none", prefs.getString("sidequestNunsCompleted", ""))
    }

    @Test
    fun applyCombatWin_logsVictoryAndAreaMessages() {
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        prefs.setString("warProgress", "started")
        prefs.setInt("hippiesDefeated", 63)
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = FRAT_BATTLEFIELD,
                responseText = winResponse(),
                won = true,
                monster = WAR_HIPPY_MONSTER,
                sessionLogger = logger,
            ),
        )
        assertEquals(64, prefs.getInt("hippiesDefeated", 0))
        val log = sessionLog(prefs)
        assertTrue(log.contains("1 frat boy defeated; 64 down, 936 left."))
        assertTrue(log.contains("The Lighthouse is now accessible in this uniform!"))
    }

    @Test
    fun finishWar_logsWarFinishedMessage() {
        val prefs = prefs()
        val db = questDb(prefs)
        val logger = sessionLogger(prefs)
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.finishWar(
                preferences = prefs,
                questDatabase = db,
                loser = "hippies",
                isKingdomOfExploathing = false,
                sessionLogger = logger,
            ),
        )
        assertTrue(sessionLog(prefs).contains("War finished: hippies defeated"))
    }
}
