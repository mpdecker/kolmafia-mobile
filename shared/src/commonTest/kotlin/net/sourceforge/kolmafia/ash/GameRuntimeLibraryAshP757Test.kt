package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BackupCameraChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP757Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_infersMeatModeAndReverser() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            BackupCameraChoiceSync.applyVisit(
                1449,
                "Warning Beep present. Disable Reverser button shown.",
                prefs,
            ),
        )
        assertEquals("meat", prefs.getString(BackupCameraChoiceSync.MODE_PREF, ""))
        assertTrue(prefs.getBoolean(BackupCameraChoiceSync.REVERSER_PREF))
    }

    @Test
    fun post_setsModesAndReverser() {
        val prefs = Preferences(MapSettings())
        assertTrue(BackupCameraChoiceSync.apply(1449, 3, prefs))
        assertEquals("init", prefs.getString(BackupCameraChoiceSync.MODE_PREF, ""))
        assertTrue(BackupCameraChoiceSync.apply(1449, 4, prefs))
        assertTrue(prefs.getBoolean(BackupCameraChoiceSync.REVERSER_PREF))
        assertTrue(BackupCameraChoiceSync.apply(1449, 5, prefs))
        assertFalse(prefs.getBoolean(BackupCameraChoiceSync.REVERSER_PREF))
    }

    @Test
    fun questChoiceRules_wires1449() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1449,
                responseText = "",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals("meat", prefs.getString(BackupCameraChoiceSync.MODE_PREF, ""))
    }
}
