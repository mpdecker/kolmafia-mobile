package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.InfernoDiscoChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP799Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_decisionAbove1_setsVisited() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            InfernoDiscoChoiceSync.apply(
                choiceId = 1090,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_infernoDiscoVisited", false))
    }

    @Test
    fun post_decision1_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            InfernoDiscoChoiceSync.apply(
                choiceId = 1090,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(false, prefs.getBoolean("_infernoDiscoVisited", false))
    }

    @Test
    fun questChoiceRules_wires1090() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1090,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 3,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_infernoDiscoVisited", false))
    }
}
