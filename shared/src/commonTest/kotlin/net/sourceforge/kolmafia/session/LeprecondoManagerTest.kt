package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class LeprecondoManagerTest {

    @Test
    fun visit_parsesInstalledAndDiscovered() {
        val prefs = Preferences(MapSettings())
        val html = """
            <img id="i0" alt="beer cooler in top left">
            <img id="i1" alt="free mattress in top right">
            You can rearrange the furnishings 2 more
            <select id="r1" name="r1"><option value='5'><option value='6'></select>
        """.trimIndent()
        LeprecondoManager.visit(html, prefs)
        assertEquals("5,6", prefs.getString("leprecondoInstalled", ""))
        assertEquals(1, prefs.getInt("_leprecondoRearrangements", -1))
        assertEquals("5,6", prefs.getString("leprecondoDiscovered", ""))
    }

    @Test
    fun handlePostCombatMessage_recordsFurnitureDiscovery() {
        val prefs = Preferences(MapSettings())
        val goal = GoalManager().also { it.setLeprecondoGoal(2) }
        val text = "Your Leprechaun spots a beer cooler and runs out of his condo."
        assertTrue(
            LeprecondoManager.handlePostCombatMessage(
                text = text,
                image = "familiar2.gif",
                preferences = prefs,
                currentRun = 12,
                goalManager = goal,
            ),
        )
        assertEquals("5", prefs.getString("leprecondoDiscovered", ""))
        assertEquals(1, prefs.getInt("_leprecondoFurniture", 0))
        assertTrue(goal.hasLeprecondoGoal())
    }

    @Test
    fun processNeedChange_tracksNeedOrder() {
        val prefs = Preferences(MapSettings())
        LeprecondoManager.processNeedChange("food", prefs, 7)
        assertEquals("food", prefs.getString("leprecondoCurrentNeed", ""))
        assertEquals(7, prefs.getInt("leprecondoLastNeedChange", -1))
        assertEquals("food", prefs.getString("leprecondoNeedOrder", ""))
    }

    @Test
    fun getUndiscoveredFurnitureForLocation_listsMissingZoneItems() {
        val prefs = Preferences(MapSettings())
        prefs.setString("leprecondoDiscovered", "5")
        val missing = LeprecondoManager.getUndiscoveredFurnitureForLocation(
            "The Orcish Frat House",
            prefs,
        )
        assertTrue(missing.contains("beer pong table"))
        assertTrue(missing.contains("couch and flatscreen"))
    }
}
