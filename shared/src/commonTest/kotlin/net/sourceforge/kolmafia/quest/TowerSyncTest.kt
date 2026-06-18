package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.adventure.TowerDoorConfig
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TowerSyncTest {

    @Test
    fun parseTower_courtyardAdvancesToStep3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, QuestDatabase.STARTED)
        val html = """<img src="nstower_courtyard.gif">"""
        assertTrue(TowerSync.parseTower(html, db, prefs))
        assertEquals("step3", db.getProgress(Quest.FINAL))
    }

    @Test
    fun parseTower_tower3ClearsContestants() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("nsContestants1", 5)
        prefs.setInt("nsContestants2", 3)
        prefs.setInt("nsContestants3", 1)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, "step1")
        val html = """<img src="nstower_tower3.gif">"""
        assertTrue(TowerSync.parseTower(html, db, prefs))
        assertEquals("step8", db.getProgress(Quest.FINAL))
        assertEquals(0, prefs.getInt("nsContestants1", -1))
        assertEquals(0, prefs.getInt("nsContestants2", -1))
        assertEquals(0, prefs.getInt("nsContestants3", -1))
    }

    @Test
    fun parseTower_gashMarksFinished() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, "step12")
        assertTrue(TowerSync.parseTower("""<img src="gash.gif">""", db, prefs))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.FINAL))
    }

    @Test
    fun parseTower_noMarkerIsNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, "step2")
        assertFalse(TowerSync.parseTower("<p>nothing here</p>", db, prefs))
        assertEquals("step2", db.getProgress(Quest.FINAL))
    }

    @Test
    fun parseTowerDoorResponse_universalKeyRecordsUniversalKeyName() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val lock = TowerDoorConfig.STANDARD_LOCKS.first { it.action == "ns_lock4" }
        val html = "You put the universal key in the lock and the lock vanishes."
        TowerSync.parseTowerDoorResponse(lock.action, html, prefs, db)
        assertTrue(TowerDoorConfig.isKeyUsed(prefs, TowerDoorConfig.UNIVERSAL_KEY_NAME))
    }
}
