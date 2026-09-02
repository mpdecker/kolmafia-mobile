package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.NumberologyChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.NumberologyRequest

class GameRuntimeLibraryAshP705Test {

    @Test
    fun revision_phase707() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun maths_incrementsUnlessTryAgain() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(NumberologyRequest.PREF_UNIVERSE_CALCULATED, 2)
        assertTrue(NumberologyChoiceSync.apply(1103, "You calculate a prize", prefs))
        assertEquals(3, prefs.getInt(NumberologyRequest.PREF_UNIVERSE_CALCULATED))
        assertFalse(NumberologyChoiceSync.apply(1103, "Try again later", prefs))
        assertEquals(3, prefs.getInt(NumberologyRequest.PREF_UNIVERSE_CALCULATED))
    }

    @Test
    fun questChoiceRules_wires1103() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1103,
                responseText = "the universe unfolds",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt(NumberologyRequest.PREF_UNIVERSE_CALCULATED))
    }
}
