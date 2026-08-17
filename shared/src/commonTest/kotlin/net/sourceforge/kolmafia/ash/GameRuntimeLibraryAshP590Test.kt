package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ExtremeSlopeSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP590Test {

    @Test
    fun extremeNc_incrementsExtremity() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("currentExtremity", 1)
        assertTrue(
            ExtremeSlopeSync.applyFromAdventure(
                "273",
                "Discovering Your Extremity",
                prefs,
            ),
        )
        assertEquals(2, prefs.getInt("currentExtremity", 0))
    }

    @Test
    fun cloudyPeak_setsTrapperAndResets() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("currentExtremity", 3)
        assertTrue(
            ExtremeSlopeSync.applyCloudyPeak(
                url = "place.php?whichplace=mclargehuge&action=cloudypeak",
                html = "you spy a crude stone staircase leading up",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("step3", db.getProgress(Quest.TRAPPER))
        assertEquals(0, prefs.getInt("currentExtremity", -1))
    }
}
