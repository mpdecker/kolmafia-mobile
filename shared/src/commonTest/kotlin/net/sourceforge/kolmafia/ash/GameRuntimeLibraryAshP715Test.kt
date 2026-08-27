package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TimeSpinnerChoiceSync

class GameRuntimeLibraryAshP715Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun spinning_incrementsMinutes() {
        val prefs = Preferences(MapSettings())
        assertTrue(TimeSpinnerChoiceSync.apply(1195, 3, prefs))
        assertEquals(1, prefs.getInt(TimeSpinnerChoiceSync.MINUTES_PREF, 0))
        assertTrue(TimeSpinnerChoiceSync.apply(1195, 4, prefs))
        assertEquals(3, prefs.getInt(TimeSpinnerChoiceSync.MINUTES_PREF, 0))
        assertFalse(TimeSpinnerChoiceSync.apply(1195, 1, prefs))
    }

    @Test
    fun questChoiceRules_wires1195() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1195,
                responseText = "",
                questDatabase = db,
                decision = 4,
                preferences = prefs,
            ),
        )
        assertEquals(2, prefs.getInt(TimeSpinnerChoiceSync.MINUTES_PREF, 0))
    }
}
