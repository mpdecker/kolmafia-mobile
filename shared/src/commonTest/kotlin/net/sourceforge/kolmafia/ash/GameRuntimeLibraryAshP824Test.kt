package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FantasyRealmChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP824Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun welcome_seedsHoursAndCrossroads() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("frMountainsUnlocked", true)
        assertTrue(FantasyRealmChoiceSync.apply(1280, 1, prefs))
        assertEquals(5, prefs.getInt("_frHoursLeft", 0))
        val areas = prefs.getString("_frAreasUnlocked", "")
        assertTrue(areas.contains("The Bandit Crossroads,"))
        assertTrue(areas.contains("The Towering Mountains,"))
    }

    @Test
    fun welcome_decision6_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(FantasyRealmChoiceSync.apply(1280, 6, prefs))
        assertEquals(0, prefs.getInt("_frHoursLeft", 0))
    }

    @Test
    fun crossroads_burnsHourAndUnlocksWood() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 5)
        prefs.setString("_frAreasUnlocked", "The Bandit Crossroads,")
        assertTrue(FantasyRealmChoiceSync.apply(1281, 2, prefs))
        assertEquals(4, prefs.getInt("_frHoursLeft", 0))
        val areas = prefs.getString("_frAreasUnlocked", "")
        assertFalse(areas.contains("The Bandit Crossroads,"))
        assertTrue(areas.contains("The Mystic Wood,"))
    }

    @Test
    fun questChoiceRules_wires1280() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1280,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(5, prefs.getInt("_frHoursLeft", 0))
    }
}
