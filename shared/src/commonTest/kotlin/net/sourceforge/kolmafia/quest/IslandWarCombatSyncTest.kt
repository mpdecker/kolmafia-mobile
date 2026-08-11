package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandWarCombatSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun winResponse(extra: String = ""): String =
        "WINWINWIN $extra".trim()

    @Test
    fun applyCombatWin_lossOnBattlefield131_noIncrement() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        prefs.setInt("hippiesDefeated", 10)
        assertFalse(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = "131",
                responseText = winResponse(),
                won = false,
            ),
        )
        assertEquals(10, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOn131WithoutMessage_incrementsByOne() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = "131",
                responseText = winResponse(),
                won = true,
            ),
        )
        assertEquals(1, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOn131WithTier2Message_incrementsByFour() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = "131",
                responseText = winResponse("You hose down three hippies with sauce."),
                won = true,
            ),
        )
        assertEquals(4, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOn131WithTier1Message_incrementsByTwo() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = "131",
                responseText = winResponse("He runs away protesting the war."),
                won = true,
            ),
        )
        assertEquals(2, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOn132WithRocketLauncher_incrementsByFour() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = "132",
                responseText = winResponse("His rocket launcher blasts 3 extra frat boys."),
                won = true,
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
                adventureId = "131",
                responseText = winResponse(),
                won = true,
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
            ),
        )
        assertEquals(0, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyCombatWin_winOn131AtKoeCap_staysAtCap() {
        val prefs = prefs()
        prefs.setString("warProgress", "started")
        prefs.setInt("hippiesDefeated", 333)
        assertFalse(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = "131",
                responseText = winResponse(),
                won = true,
                isKingdomOfExploathing = true,
            ),
        )
        assertEquals(333, prefs.getInt("hippiesDefeated", 0))
    }

    private fun questDb(prefs: Preferences = prefs()): QuestDatabase = QuestDatabase(prefs)

    @Test
    fun applyEndOfWar_bigWisniewskiOn131_finishesIslandWar() {
        val prefs = prefs()
        val db = questDb(prefs)
        prefs.setString("warProgress", "started")
        prefs.setInt("hippiesDefeated", 50)
        assertTrue(
            IslandWarCombatSync.applyEndOfWar(
                questDatabase = db,
                preferences = prefs,
                adventureId = "131",
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
    fun applyEndOfWar_theManOn132KoE_finishesHippyFrat() {
        val prefs = prefs()
        val db = questDb(prefs)
        prefs.setString("warProgress", "started")
        assertTrue(
            IslandWarCombatSync.applyEndOfWar(
                questDatabase = db,
                preferences = prefs,
                adventureId = "132",
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
                monster = "War Frat Wartender",
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
                adventureId = "131",
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
                adventureId = "131",
                monster = "The Big Wisniewski",
                responseText = winResponse("He runs away protesting the war."),
                won = true,
            ),
        )
        assertEquals(1000, prefs.getInt("hippiesDefeated", 0))
        assertFalse(
            IslandWarCombatSync.applyCombatWin(
                preferences = prefs,
                adventureId = "131",
                responseText = winResponse("He runs away protesting the war."),
                won = true,
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
                monster = "War Frat Wartender",
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
}
