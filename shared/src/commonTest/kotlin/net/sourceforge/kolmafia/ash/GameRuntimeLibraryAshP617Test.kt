package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FernruinVisitSync
import net.sourceforge.kolmafia.quest.GarbageBeanstalkSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP617Test {

    @Test
    fun revision_phase623() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun beanstalkGif_setsGarbageStep1() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(
            GarbageBeanstalkSync.applyFromPlace(
                url = "place.php?whichplace=beanstalk",
                html = """<img src="otherimages/stalktop/beanstalk.gif">""",
                questDatabase = db,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.GARBAGE))
    }

    @Test
    fun fernruin_setsEgoStep3WithoutKey() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(FernruinVisitSync.applyFromVisit("fernruin.php", db))
        assertEquals("step3", db.getProgress(Quest.EGO))
    }

    @Test
    fun fernruin_doesNotRegressFinished() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.EGO, QuestDatabase.FINISHED)
        assertTrue(FernruinVisitSync.applyFromVisit("fernruin.php", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.EGO))
    }

    @Test
    fun otherPlace_isNoOp() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertFalse(
            GarbageBeanstalkSync.applyFromPlace(
                url = "place.php?whichplace=plains",
                html = """<img src="otherimages/stalktop/beanstalk.gif">""",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.GARBAGE))
    }
}
