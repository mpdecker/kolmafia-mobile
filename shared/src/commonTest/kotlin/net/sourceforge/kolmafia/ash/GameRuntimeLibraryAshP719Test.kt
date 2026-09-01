package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SeaJellyChoiceSync

class GameRuntimeLibraryAshP719Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun jelly_setsOnDecision1() {
        val prefs = Preferences(MapSettings())
        assertFalse(SeaJellyChoiceSync.apply(1219, 2, prefs))
        assertFalse(prefs.getBoolean("_seaJellyHarvested"))
        assertTrue(SeaJellyChoiceSync.apply(1219, 1, prefs))
        assertTrue(prefs.getBoolean("_seaJellyHarvested"))
    }

    @Test
    fun questChoiceRules_wires1219() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1219,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_seaJellyHarvested"))
    }
}
