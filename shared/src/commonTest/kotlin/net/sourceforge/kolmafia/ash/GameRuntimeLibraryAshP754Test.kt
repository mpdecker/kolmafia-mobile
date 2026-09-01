package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BwApronChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP754Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun consume_incrementsKnownMeals() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(BwApronChoiceSync.MEALS_PREF, 2)
        var consumed = 0
        assertTrue(
            BwApronChoiceSync.apply(
                1518,
                "You cook and quickly consume your meal.",
                prefs,
                consumeItem = { id, qty ->
                    assertEquals(BwApronChoiceSync.MEAL_KIT, id)
                    consumed = qty
                },
            ),
        )
        assertEquals(1, consumed)
        assertEquals(3, prefs.getInt(BwApronChoiceSync.MEALS_PREF, 0))
    }

    @Test
    fun unknownMeals_notIncremented() {
        val prefs = Preferences(MapSettings())
        // default -1
        assertTrue(
            BwApronChoiceSync.apply(1518, "You cook and quickly consume your meal.", prefs),
        )
        assertEquals(-1, prefs.getInt(BwApronChoiceSync.MEALS_PREF, -1))
    }

    @Test
    fun questChoiceRules_wires1518() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(BwApronChoiceSync.MEALS_PREF, 0)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1518,
                responseText = "You cook and quickly consume your",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt(BwApronChoiceSync.MEALS_PREF, 0))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(
            BwApronChoiceSync.apply(1406, "You cook and quickly consume your", Preferences(MapSettings())),
        )
    }
}
