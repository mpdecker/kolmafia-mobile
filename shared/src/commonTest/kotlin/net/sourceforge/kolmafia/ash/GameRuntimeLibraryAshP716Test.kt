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

class GameRuntimeLibraryAshP716Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun recentFight_skipsMonidZero() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            TimeSpinnerChoiceSync.apply(
                1196,
                1,
                prefs,
                choiceUrl = "choice.php?whichchoice=1196&option=1&monid=0",
            ),
        )
        assertEquals(0, prefs.getInt(TimeSpinnerChoiceSync.MINUTES_PREF, 0))
    }

    @Test
    fun recentFight_incrementsThree() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TimeSpinnerChoiceSync.apply(
                1196,
                1,
                prefs,
                choiceUrl = "choice.php?whichchoice=1196&option=1&monid=123",
            ),
        )
        assertEquals(3, prefs.getInt(TimeSpinnerChoiceSync.MINUTES_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1196() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1196,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                choiceUrl = "monid=42",
            ),
        )
        assertEquals(3, prefs.getInt(TimeSpinnerChoiceSync.MINUTES_PREF, 0))
    }
}
