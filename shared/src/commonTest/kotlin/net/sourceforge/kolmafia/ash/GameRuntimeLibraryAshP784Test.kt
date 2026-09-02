package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PerilChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP784Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesRemaining() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            PerilChoiceSync.applyVisit(
                1558,
                "You can foresee peril 2 more times today.",
                prefs,
            ),
        )
        assertEquals(1, prefs.getInt("_perilsForeseen", -1))
    }

    @Test
    fun post_incrementsOnGaze() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_perilsForeseen", 1)
        assertTrue(
            PerilChoiceSync.apply(
                1558,
                "You gaze into your Peridot and foresee a horrible future",
                prefs,
            ),
        )
        assertEquals(2, prefs.getInt("_perilsForeseen", 0))
    }

    @Test
    fun questChoiceRules_wires1558() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1558,
                responseText = "You've already seen too much peril.",
                questDatabase = QuestDatabase(prefs),
                preferences = prefs,
            ),
        )
        assertEquals(3, prefs.getInt("_perilsForeseen", 0))
    }
}
