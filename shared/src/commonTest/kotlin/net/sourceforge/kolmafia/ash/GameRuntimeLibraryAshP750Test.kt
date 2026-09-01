package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DrippyTreeChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP750Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun bats_unlockIncrement() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DrippyTreeChoiceSync.apply(
                1406,
                "You flush some of the vile bat-things out",
                prefs,
            ),
        )
        assertEquals(7, prefs.getInt(DrippyTreeChoiceSync.BATS_PREF, 0))
        assertEquals(16, prefs.getInt(DrippyTreeChoiceSync.ADVENTURES_PREF, 0))
    }

    @Test
    fun stake_discardsTruncheon() {
        val prefs = Preferences(MapSettings())
        var discarded = 0
        assertTrue(
            DrippyTreeChoiceSync.apply(
                1406,
                "You carve your truncheon into a sharp stake.",
                prefs,
                discardItem = { id ->
                    assertEquals(DrippyTreeChoiceSync.DRIPPY_TRUNCHEON, id)
                    discarded = id
                },
            ),
        )
        assertEquals(DrippyTreeChoiceSync.DRIPPY_TRUNCHEON, discarded)
    }

    @Test
    fun schedule_padsOntoFifteenCycle() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(DrippyTreeChoiceSync.ADVENTURES_PREF, 18)
        assertTrue(DrippyTreeChoiceSync.apply(1406, "explore", prefs))
        // (18-1)%15 = 2 → pad by 13 → 31
        assertEquals(31, prefs.getInt(DrippyTreeChoiceSync.ADVENTURES_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1406() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1406,
                responseText = "vile bat-things",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(7, prefs.getInt(DrippyTreeChoiceSync.BATS_PREF, 0))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(DrippyTreeChoiceSync.apply(1411, "vile bat-things", Preferences(MapSettings())))
    }
}
