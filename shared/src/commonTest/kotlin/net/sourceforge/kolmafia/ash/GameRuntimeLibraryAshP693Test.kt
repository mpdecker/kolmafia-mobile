package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ManorTowelChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP693Test {

    @Test
    fun revision_phase695() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun towel_setsLastAscension() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            ManorTowelChoiceSync.apply(
                choiceId = 882,
                decision = 1,
                html = "You never know when it might come in handy.",
                preferences = prefs,
                ascensionNumber = 7,
            ),
        )
        assertEquals(7, prefs.getInt("lastTowelAscension"))
    }

    @Test
    fun towel_requiresDecision1() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            ManorTowelChoiceSync.apply(
                choiceId = 882,
                decision = 2,
                html = "You never know when it might come in handy.",
                preferences = prefs,
                ascensionNumber = 7,
            ),
        )
    }

    @Test
    fun questChoiceRules_wires882() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 882,
                responseText = "You never know when it might come in handy.",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                ascensionNumber = 4,
            ),
        )
        assertEquals(4, prefs.getInt("lastTowelAscension"))
    }
}
