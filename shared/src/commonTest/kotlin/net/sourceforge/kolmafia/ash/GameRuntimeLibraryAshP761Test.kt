package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.JuneCleaverChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP761Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun encounter_updatesQueueAndFights() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            JuneCleaverChoiceSync.apply(
                choiceId = 1468,
                decision = 1,
                preferences = prefs,
                choiceUrl = "choice.php?whichchoice=1468&option=1",
            ),
        )
        assertEquals("1468", prefs.getString("juneCleaverQueue", ""))
        assertEquals(1, prefs.getInt("_juneCleaverEncounters", 0))
        assertEquals(6, prefs.getInt("_juneCleaverFightsLeft", 0))
    }

    @Test
    fun skip_usesResetSchedule() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            JuneCleaverChoiceSync.apply(
                1470,
                4,
                prefs,
                "whichchoice=1470&option=4",
            ),
        )
        assertEquals(1, prefs.getInt("_juneCleaverSkips", 0))
        assertEquals(1, prefs.getInt("_juneCleaverFightsLeft", 0))
    }

    @Test
    fun poeticJustice_tracksAdvs() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            JuneCleaverChoiceSync.apply(
                1467,
                3,
                prefs,
                "whichchoice=1467&option=3",
            ),
        )
        assertEquals(5, prefs.getInt("_juneCleaverAdvs", 0))
    }

    @Test
    fun questChoiceRules_wires1467() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1467,
                responseText = "",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
                choiceUrl = "whichchoice=1467&option=2",
            ),
        )
        assertEquals(1, prefs.getInt("_juneCleaverEncounters", 0))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(
            JuneCleaverChoiceSync.apply(1451, 1, Preferences(MapSettings()), "whichchoice=1451&option=1"),
        )
    }
}
