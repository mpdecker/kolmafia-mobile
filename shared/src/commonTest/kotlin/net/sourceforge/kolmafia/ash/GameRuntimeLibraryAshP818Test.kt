package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.GnasirChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP818Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_stoneRoseBitflag() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            GnasirChoiceSync.apply(
                choiceId = 805,
                html = "You give the stone rose to Gnasir.",
                preferences = prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(1, prefs.getInt("gnasirProgress", 0))
        assertEquals(listOf(GnasirChoiceSync.STONE_ROSE to 1), consumed)
    }

    @Test
    fun post_pagesBitflagOrs() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("gnasirProgress", 1)
        assertTrue(
            GnasirChoiceSync.apply(
                choiceId = 805,
                html = "You hand him the pages, and he shuffles them",
                preferences = prefs,
            ),
        )
        assertEquals(1 or 8, prefs.getInt("gnasirProgress", 0))
    }

    @Test
    fun questChoiceRules_wires805() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 805,
                responseText = "hold up the bucket of black paint",
                questDatabase = QuestDatabase(prefs),
                preferences = prefs,
            ),
        )
        assertEquals(2, prefs.getInt("gnasirProgress", 0))
    }
}
