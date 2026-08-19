package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PandamoniumVisitSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP614Test {

    @Test
    fun revision_phase614() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_startsAzazel() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(PandamoniumVisitSync.applyFromVisit("pandamonium.php", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.AZAZEL))
    }

    @Test
    fun alreadyStep1_stays() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.AZAZEL, "step1")
        assertTrue(PandamoniumVisitSync.applyFromVisit("pandamonium.php?action=sven", db))
        assertEquals("step1", db.getProgress(Quest.AZAZEL))
    }

    @Test
    fun otherUrl_isNoOp() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertFalse(PandamoniumVisitSync.applyFromVisit("council.php", db))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.AZAZEL))
    }
}
