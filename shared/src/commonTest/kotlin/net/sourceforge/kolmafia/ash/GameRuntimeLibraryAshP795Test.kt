package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.RetroCapeChoiceSync

class GameRuntimeLibraryAshP795Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_washInstructions() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            RetroCapeChoiceSync.apply(
                choiceId = 1437,
                decision = 5,
                preferences = prefs,
            ),
        )
        assertEquals("kill", prefs.getString("retroCapeWashingInstructions", ""))
    }

    @Test
    fun post_superhero() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            RetroCapeChoiceSync.apply(
                choiceId = 1438,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals("heck", prefs.getString("retroCapeSuperhero", ""))
    }

    @Test
    fun questChoiceRules_wires1437() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1437,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 3,
                preferences = prefs,
            ),
        )
        assertEquals("thrill", prefs.getString("retroCapeWashingInstructions", ""))
    }
}
