package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DmtChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP694Test {

    @Test
    fun revision_phase695() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_resetsEncounterCounter() {
        val prefs = Preferences(MapSettings())
        assertTrue(DmtChoiceSync.apply(1119, 1, prefs, 3))
        assertEquals(49, prefs.getInt("encountersUntilDMTChoice"))
        assertEquals(-1, prefs.getInt("lastDMTDuplication", -1))
    }

    @Test
    fun decision4_setsDuplicationOnce() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastDMTDuplication", 2)
        assertTrue(DmtChoiceSync.apply(1119, 4, prefs, 3))
        assertEquals(3, prefs.getInt("lastDMTDuplication"))
        assertTrue(DmtChoiceSync.apply(1119, 4, prefs, 3))
        assertEquals(3, prefs.getInt("lastDMTDuplication"))
    }

    @Test
    fun questChoiceRules_wires1119() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1119,
                responseText = "",
                questDatabase = db,
                decision = 4,
                preferences = prefs,
                ascensionNumber = 9,
            ),
        )
        assertEquals(49, prefs.getInt("encountersUntilDMTChoice"))
        assertEquals(9, prefs.getInt("lastDMTDuplication"))
    }
}
