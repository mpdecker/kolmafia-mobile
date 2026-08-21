package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MappingMonstersChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP794Test {

    @Test
    fun revision_phase826() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_decision1_clearsMapping() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("mappingMonsters", true)
        assertTrue(
            MappingMonstersChoiceSync.apply(
                choiceId = 1435,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(false, prefs.getBoolean("mappingMonsters", true))
    }

    @Test
    fun post_otherDecision_noop() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("mappingMonsters", true)
        assertFalse(
            MappingMonstersChoiceSync.apply(
                choiceId = 1435,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("mappingMonsters", false))
    }

    @Test
    fun questChoiceRules_wires1435() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("mappingMonsters", true)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1435,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(false, prefs.getBoolean("mappingMonsters", true))
    }
}
