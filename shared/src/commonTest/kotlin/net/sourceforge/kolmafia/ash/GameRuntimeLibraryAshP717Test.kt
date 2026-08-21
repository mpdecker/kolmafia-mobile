package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.GingerbreadClockChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP717Test {

    @Test
    fun revision_phase814() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun clock_alwaysMarksVisited() {
        val prefs = Preferences(MapSettings())
        assertTrue(GingerbreadClockChoiceSync.apply(1215, 2, prefs))
        assertTrue(prefs.getBoolean("_gingerbreadClockVisited"))
        assertFalse(prefs.getBoolean("_gingerbreadClockAdvanced"))
        assertEquals(0, prefs.getInt("_gingerbreadCityTurns", 0))
    }

    @Test
    fun clock_decision1_advances() {
        val prefs = Preferences(MapSettings())
        assertTrue(GingerbreadClockChoiceSync.apply(1215, 1, prefs))
        assertTrue(prefs.getBoolean("_gingerbreadClockVisited"))
        assertTrue(prefs.getBoolean("_gingerbreadClockAdvanced"))
        assertEquals(1, prefs.getInt("_gingerbreadCityTurns", 0))
    }

    @Test
    fun questChoiceRules_wires1215() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1215,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_gingerbreadClockAdvanced"))
    }
}
