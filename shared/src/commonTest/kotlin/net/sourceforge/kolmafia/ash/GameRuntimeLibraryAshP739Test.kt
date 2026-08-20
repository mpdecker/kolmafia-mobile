package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AutomatedFutureChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP739Test {

    @Test
    fun visit_oppositeSide() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            AutomatedFutureChoiceSync.applyVisit(
                1512,
                "don't even think about pressing that button",
                prefs,
            ),
        )
        assertEquals("bearings", prefs.getString(AutomatedFutureChoiceSync.SIDE_PREF, ""))
    }

    @Test
    fun visit_elevenTimes() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            AutomatedFutureChoiceSync.applyVisit(
                1513,
                "You've already pushed the button eleven times today",
                prefs,
            ),
        )
        assertEquals("bearings", prefs.getString(AutomatedFutureChoiceSync.SIDE_PREF, ""))
        assertEquals(11, prefs.getInt(AutomatedFutureChoiceSync.MANUFACTURES_PREF, 0))
    }

    @Test
    fun post_pressesButton() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            AutomatedFutureChoiceSync.apply(
                choiceId = 1512,
                decision = 1,
                html = "You press the button.",
                preferences = prefs,
            ),
        )
        assertEquals("solenoids", prefs.getString(AutomatedFutureChoiceSync.SIDE_PREF, ""))
        assertEquals(1, prefs.getInt(AutomatedFutureChoiceSync.MANUFACTURES_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1513() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1513,
                responseText = "You press the button.",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals("bearings", prefs.getString(AutomatedFutureChoiceSync.SIDE_PREF, ""))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            AutomatedFutureChoiceSync.apply(1219, 1, "You press the button.", prefs),
        )
    }
}
