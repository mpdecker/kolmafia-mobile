package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BurningLeavesChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP769Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesBurnedAndJump() {
        val prefs = Preferences(MapSettings())
        val html = """You've stoked the fire with <b>3</b> random leaves today.
            Jump in the Flames""".trimIndent()
        assertTrue(BurningLeavesChoiceSync.applyVisit(1510, html, prefs))
        assertEquals(3, prefs.getInt("_leavesBurned", 0))
        assertEquals(false, prefs.getBoolean("_leavesJumped", true))
    }

    @Test
    fun post_randomLeavesIncrements() {
        val prefs = Preferences(MapSettings())
        var consumed = 0
        assertTrue(
            BurningLeavesChoiceSync.apply(
                choiceId = 1510,
                html = "The leaves burn brightly.",
                preferences = prefs,
                choiceUrl = "leaves=5",
                consumeItem = { id, qty -> if (id == 11341) consumed += qty },
            ),
        )
        assertEquals(5, prefs.getInt("_leavesBurned", 0))
        assertEquals(5, consumed)
    }

    @Test
    fun post_lassoDailyPref() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            BurningLeavesChoiceSync.apply(
                choiceId = 1510,
                html = "You craft a lasso.",
                preferences = prefs,
                choiceUrl = "leaves=69",
            ),
        )
        assertEquals(1, prefs.getInt("_leafLassosCrafted", 0))
    }

    @Test
    fun post_jumpSetsFlag() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            BurningLeavesChoiceSync.apply(
                choiceId = 1510,
                html = "You jump in the blazing fire absorb some of the flames and jump out",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_leavesJumped", false))
    }

    @Test
    fun questChoiceRules_wires1510() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1510,
                responseText = "ok",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                choiceUrl = "leaves=2",
            ),
        )
        assertEquals(2, prefs.getInt("_leavesBurned", 0))
    }
}
