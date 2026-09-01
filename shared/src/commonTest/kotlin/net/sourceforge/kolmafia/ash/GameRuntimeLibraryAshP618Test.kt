package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TavernVisitSync

class GameRuntimeLibraryAshP618Test {

    @Test
    fun revision_phase623() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun houseDrinks_finishRat() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.RAT, "step2")
        assertTrue(
            TavernVisitSync.applyFromVisit(
                url = "tavern.php",
                html = "You have a few drinks on the house.",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.RAT))
    }

    @Test
    fun barkeepVisit_startsStep1() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(
            TavernVisitSync.applyFromVisit(
                url = "tavern.php?place=barkeep",
                html = "Bart Ender nods.",
                questDatabase = db,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.RAT))
    }

    @Test
    fun barkeepSwill_finishes() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(
            TavernVisitSync.applyFromVisit(
                url = "tavern.php?place=barkeep",
                html = "grab some mugs and pour yourself some tavern swill",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.RAT))
    }

    @Test
    fun alreadyFinished_staysOnBarkeep() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.RAT, QuestDatabase.FINISHED)
        assertTrue(
            TavernVisitSync.applyFromVisit(
                url = "tavern.php?place=barkeep",
                html = "Bart Ender nods.",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.RAT))
    }

    @Test
    fun otherUrl_isNoOp() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertFalse(
            TavernVisitSync.applyFromVisit(
                url = "council.php",
                html = "You have a few drinks on the house.",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.RAT))
    }
}
