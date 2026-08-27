package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ClancyNcSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP588Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun barroom_jukebox_setsClancyStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ClancyNcSync.applyFromAdventure("233", "Jackin' the Jukebox plays", db),
        )
        assertEquals("step1", db.getProgress(Quest.CLANCY))
    }

    @Test
    fun knobShaft_miner_setsStep3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ClancyNcSync.applyFromAdventure("101", "A Miner Variation", db),
        )
        assertEquals("step3", db.getProgress(Quest.CLANCY))
    }

    @Test
    fun icyPeak_mercury_setsStep7() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ClancyNcSync.applyFromAdventure("110", "Mercury Rising", db),
        )
        assertEquals("step7", db.getProgress(Quest.CLANCY))
    }
}
