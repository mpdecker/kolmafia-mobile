package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AntiScientificChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP779Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun smash_appendsLocation() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            AntiScientificChoiceSync.apply(
                choiceId = 1522,
                html = "You smashed scientific equipment here.",
                preferences = prefs,
                lastVisitedLocationName = "The Laboratory",
            ),
        )
        assertEquals("The Laboratory", prefs.getString("antiScientificMethod", ""))
        assertTrue(
            AntiScientificChoiceSync.apply(
                choiceId = 1522,
                html = "smashed scientific equipment again",
                preferences = prefs,
                lastVisitedLocationName = "The Factory",
            ),
        )
        assertEquals("The Laboratory|The Factory", prefs.getString("antiScientificMethod", ""))
    }

    @Test
    fun withoutSmashText_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            AntiScientificChoiceSync.apply(
                1522,
                "nothing special",
                prefs,
                lastVisitedLocationName = "Somewhere",
            ),
        )
    }

    @Test
    fun questChoiceRules_wires1522() {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_LOCATION, "The Lab")
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1522,
                responseText = "smashed scientific equipment",
                questDatabase = QuestDatabase(prefs),
                preferences = prefs,
            ),
        )
        assertEquals("The Lab", prefs.getString("antiScientificMethod", ""))
    }
}
