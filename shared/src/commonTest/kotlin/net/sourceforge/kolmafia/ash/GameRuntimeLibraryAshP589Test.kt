package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TowerRuinsSync

class GameRuntimeLibraryAshP589Test {

    @Test
    fun dustyLook_setsStep6() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            TowerRuinsSync.applyFromAdventure("22", "Take a Dusty Look!", db),
        )
        assertEquals("step6", db.getProgress(Quest.EGO))
    }

    @Test
    fun maw_setsStep5() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            TowerRuinsSync.applyFromAdventure("22", "Into the Maw of Deepness", db),
        )
        assertEquals("step5", db.getProgress(Quest.EGO))
    }

    @Test
    fun staring_setsStep4() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            TowerRuinsSync.applyFromAdventure("22", "Staring into Nothing", db),
        )
        assertEquals("step4", db.getProgress(Quest.EGO))
    }

    @Test
    fun default_setsStep3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            TowerRuinsSync.applyFromAdventure("22", "You explore the ruins.", db),
        )
        assertEquals("step3", db.getProgress(Quest.EGO))
    }

    @Test
    fun finished_shortCircuits() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.EGO, QuestDatabase.FINISHED)
        assertFalse(
            TowerRuinsSync.applyFromAdventure("22", "Take a Dusty Look!", db),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.EGO))
    }
}
