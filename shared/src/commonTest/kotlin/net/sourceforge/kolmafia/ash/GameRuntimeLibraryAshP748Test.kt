package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AwolChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP748Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun snakeOiler_setsMedicineAndVenom() {
        val prefs = Preferences(MapSettings())
        assertTrue(AwolChoiceSync.apply(1176, 3, prefs))
        assertEquals(3, prefs.getInt("awolMedicine", 0))
        assertEquals(3, prefs.getInt("awolVenom", 0))
    }

    @Test
    fun otherDecisions_ignored() {
        val prefs = Preferences(MapSettings())
        assertFalse(AwolChoiceSync.apply(1176, 1, prefs))
        assertEquals(0, prefs.getInt("awolMedicine", 0))
    }

    @Test
    fun questChoiceRules_wires1176() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1176,
                responseText = "",
                questDatabase = db,
                decision = 3,
                preferences = prefs,
            ),
        )
        assertEquals(3, prefs.getInt("awolVenom", 0))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(AwolChoiceSync.apply(1491, 3, Preferences(MapSettings())))
    }
}
