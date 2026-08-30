package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.HybridizationChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP817Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_clearsMatchingFamiliarAndRefreshes() {
        var cleared = false
        var refreshed = false
        assertTrue(
            HybridizationChoiceSync.apply(
                choiceId = 1553,
                decision = 1,
                html = "<span class='guts'>Grafting something</span>",
                choiceUrl = "fam=77",
                currentFamiliarId = { 77 },
                clearActiveFamiliar = { cleared = true },
                refreshStatus = { refreshed = true },
            ),
        )
        assertTrue(cleared)
        assertTrue(refreshed)
    }

    @Test
    fun post_otherFamiliar_skipsClear() {
        var cleared = false
        assertTrue(
            HybridizationChoiceSync.apply(
                choiceId = 1553,
                decision = 1,
                html = "<span class='guts'>Grafting</span>",
                choiceUrl = "fam=77",
                currentFamiliarId = { 12 },
                clearActiveFamiliar = { cleared = true },
            ),
        )
        assertFalse(cleared)
    }

    @Test
    fun questChoiceRules_wires1553() {
        val prefs = Preferences(MapSettings())
        var refreshed = false
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1553,
                responseText = "<span class='guts'>Grafting</span>",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                choiceUrl = "fam=1",
                refreshStatus = { refreshed = true },
            ),
        )
        assertTrue(refreshed)
    }
}
