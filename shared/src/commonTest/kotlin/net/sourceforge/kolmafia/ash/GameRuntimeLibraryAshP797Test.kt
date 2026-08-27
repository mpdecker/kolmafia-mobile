package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CatBurglarChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP797Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_decision1_incrementsHeists() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(CatBurglarChoiceSync.HEISTS_PREF, 2)
        assertTrue(
            CatBurglarChoiceSync.apply(
                choiceId = 1320,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(3, prefs.getInt(CatBurglarChoiceSync.HEISTS_PREF, 0))
    }

    @Test
    fun post_otherDecision_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            CatBurglarChoiceSync.apply(
                choiceId = 1320,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(0, prefs.getInt(CatBurglarChoiceSync.HEISTS_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1320() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1320,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt(CatBurglarChoiceSync.HEISTS_PREF, 0))
    }
}
