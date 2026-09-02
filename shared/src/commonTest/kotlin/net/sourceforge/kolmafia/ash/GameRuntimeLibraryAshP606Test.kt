package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SeaVisitSync

class GameRuntimeLibraryAshP606Test {

    @Test
    fun revision_phase606() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun oldman_startsOldGuyQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaVisitSync.applyFromVisit(
                url = "place.php?whichplace=sea_oldman&action=oldman_oldman",
                html = "I lost my favorite boot, you see.",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.SEA_OLD_GUY))
    }

    @Test
    fun littleBrother_setsStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaVisitSync.applyFromVisit(
                url = "monkeycastle.php?who=1",
                html = "wish my big brother was here",
                questDatabase = db,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.SEA_MONKEES))
    }

    @Test
    fun grandpa_setsStep6() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaVisitSync.applyFromVisit(
                url = "monkeycastle.php?action=grandpastory",
                html = "bet those lousy Mer-kin up and kidnapped her",
                questDatabase = db,
            ),
        )
        assertEquals("step6", db.getProgress(Quest.SEA_MONKEES))
    }

    @Test
    fun grandpa_seahorseLine_unlocksCorral() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaVisitSync.applyFromVisit(
                url = "monkeycastle.php?action=grandpastory",
                html = "Gonna need one of them seahorses",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("corralUnlocked", false))
    }

    @Test
    fun outpost_setsStep9() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaVisitSync.applyFromAdventure(
                adventureId = SeaVisitSync.MERKIN_OUTPOST.toString(),
                html = "Phew, that was a close one",
                questDatabase = db,
            ),
        )
        assertEquals("step9", db.getProgress(Quest.SEA_MONKEES))
    }

    @Test
    fun abyss_finishesMonkees() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaVisitSync.applyFromAdventure(
                adventureId = SeaVisitSync.CALIGINOUS_ABYSS.toString(),
                html = "I should get dinner on the table for the boys",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.SEA_MONKEES))
    }
}
