package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PlainsVisitSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP600Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun plant_setsGarbageStep1AndConsumesBean() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            PlainsVisitSync.applyFromVisit(
                url = "place.php?whichplace=plains",
                html = "immediately grows into an enormous beanstalk",
                questDatabase = db,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals("step1", db.getProgress(Quest.GARBAGE))
        assertEquals(listOf(PlainsVisitSync.ENCHANTED_BEAN to 1), consumed)
    }

    @Test
    fun palinlink_startsPalindome() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            PlainsVisitSync.applyFromVisit(
                url = "place.php?whichplace=plains",
                html = """<img src="palinlink.gif">""",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.PALINDOME))
    }
}
