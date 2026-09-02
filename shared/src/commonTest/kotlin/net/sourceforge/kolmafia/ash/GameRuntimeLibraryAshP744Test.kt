package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MushyCenterChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP744Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun fertilize_incrementsLevel() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(MushyCenterChoiceSync.LEVEL_PREF, 3)
        assertTrue(MushyCenterChoiceSync.apply(1410, 1, prefs))
        assertEquals(4, prefs.getInt(MushyCenterChoiceSync.LEVEL_PREF, 0))
        assertTrue(prefs.getBoolean(MushyCenterChoiceSync.VISITED_PREF))
    }

    @Test
    fun pick_resetsLevel() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(MushyCenterChoiceSync.LEVEL_PREF, 8)
        assertTrue(MushyCenterChoiceSync.apply(1410, 2, prefs))
        assertEquals(1, prefs.getInt(MushyCenterChoiceSync.LEVEL_PREF, 0))
    }

    @Test
    fun visit_parsesBulkyMushroom() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            MushyCenterChoiceSync.applyVisit(
                1410,
                "A bulky mushroom and mushgrow3.gif",
                prefs,
            ),
        )
        assertEquals(3, prefs.getInt(MushyCenterChoiceSync.LEVEL_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1410() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1410,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt(MushyCenterChoiceSync.LEVEL_PREF, 0))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(MushyCenterChoiceSync.apply(1266, 1, Preferences(MapSettings())))
    }
}
