package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MadnessBakeryChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP691Test {

    @Test
    fun revision_phase695() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun baguette_consumesOnDecision1to3() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            MadnessBakeryChoiceSync.apply(
                choiceId = 1081,
                decision = 2,
                html = "",
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(MadnessBakeryChoiceSync.MAGICAL_BAGUETTE to 1))
    }

    @Test
    fun baguette_skipsDecision4() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertFalse(
            MadnessBakeryChoiceSync.apply(
                choiceId = 1081,
                decision = 4,
                html = "",
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun questChoiceRules_wires1081() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1081,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
    }
}
