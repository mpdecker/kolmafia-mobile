package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PhotoBoothChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP755Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun effect_incrementsOnSelect() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            PhotoBoothChoiceSync.apply(1534, 1, "You select wild style", prefs),
        )
        assertEquals(1, prefs.getInt(PhotoBoothChoiceSync.EFFECTS_PREF, 0))
    }

    @Test
    fun effect_leaveIgnored() {
        assertFalse(
            PhotoBoothChoiceSync.apply(1534, 6, "You select", Preferences(MapSettings())),
        )
    }

    @Test
    fun prop_incrementsOnGrab() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            PhotoBoothChoiceSync.apply(1535, 2, "You grab your prop", prefs),
        )
        assertEquals(1, prefs.getInt(PhotoBoothChoiceSync.EQUIPMENT_PREF, 0))
    }

    @Test
    fun prop_leaveIgnored() {
        assertFalse(
            PhotoBoothChoiceSync.apply(1535, 12, "You grab your prop", Preferences(MapSettings())),
        )
    }

    @Test
    fun questChoiceRules_wires1534And1535() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1534,
                responseText = "You select",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1535,
                responseText = "You grab your prop",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt(PhotoBoothChoiceSync.EFFECTS_PREF, 0))
        assertEquals(1, prefs.getInt(PhotoBoothChoiceSync.EQUIPMENT_PREF, 0))
    }
}
