package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.WoolChoiceSync

class GameRuntimeLibraryAshP774Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun slagging_consumesShards() {
        val prefs = Preferences(MapSettings())
        var consumed = 0
        assertTrue(
            WoolChoiceSync.apply(1489, 1, prefs) { id, qty ->
                if (id == WoolChoiceSync.CRIMBO_CRYSTAL_SHARDS_ID) consumed += qty
            },
        )
        assertEquals(1, consumed)
    }

    @Test
    fun wool_consumesGrubbyWool() {
        val prefs = Preferences(MapSettings())
        var consumed = 0
        assertTrue(
            WoolChoiceSync.apply(1490, 3, prefs) { id, qty ->
                if (id == WoolChoiceSync.GRUBBY_WOOL_ID) consumed += qty
            },
        )
        assertEquals(1, consumed)
    }

    @Test
    fun questChoiceRules_wires1490() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1490,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 6,
                preferences = prefs,
            ),
        )
    }
}
