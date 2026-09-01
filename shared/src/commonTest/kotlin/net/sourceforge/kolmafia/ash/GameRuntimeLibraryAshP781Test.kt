package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CandyDevilerChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP781Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun devil_incrementsAndConsumes() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            CandyDevilerChoiceSync.apply(
                choiceId = 1544,
                html = "You place your candy in the deviler.",
                preferences = prefs,
                choiceUrl = "a=2300",
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(1, prefs.getInt("_candyEggsDeviled", 0))
        assertEquals(listOf(2300 to 1), consumed)
    }

    @Test
    fun questChoiceRules_wires1544() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1544,
                responseText = "You place your candy in the deviler",
                questDatabase = QuestDatabase(prefs),
                preferences = prefs,
                choiceUrl = "a=99",
            ),
        )
        assertEquals(1, prefs.getInt("_candyEggsDeviled", 0))
    }
}
