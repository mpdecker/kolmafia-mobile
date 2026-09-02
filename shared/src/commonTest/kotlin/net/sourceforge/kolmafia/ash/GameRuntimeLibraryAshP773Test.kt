package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ElfGratitudeChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP773Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun caboose_option2_addsThree() {
        val prefs = Preferences(MapSettings())
        assertTrue(ElfGratitudeChoiceSync.apply(1486, 2, prefs))
        assertEquals(3, prefs.getInt("elfGratitude", 0))
        assertFalse(ElfGratitudeChoiceSync.apply(1486, 1, prefs))
    }

    @Test
    fun passenger_addsFive() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("elfGratitude", 10)
        assertTrue(ElfGratitudeChoiceSync.apply(1487, 1, prefs))
        assertEquals(15, prefs.getInt("elfGratitude", 0))
    }

    @Test
    fun questChoiceRules_wires1487() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1487,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(5, prefs.getInt("elfGratitude", 0))
    }
}
