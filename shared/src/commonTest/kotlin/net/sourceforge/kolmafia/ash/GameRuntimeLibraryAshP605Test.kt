package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ElVibratoSync
import net.sourceforge.kolmafia.request.PortalRequest

class GameRuntimeLibraryAshP605Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun reopen_setsEnergyFromSphere() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            PortalRequest.parseResponse(
                url = "campground.php?action=powerelvibratoportal",
                html = "The pieces of the device rise from the ground",
                preferences = prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(5, prefs.getInt("currentPortalEnergy", 0))
        assertEquals(5, CampgroundInventorySync.load(prefs)[ElVibratoSync.TRAPEZOID])
        assertEquals(listOf(PortalRequest.POWER_SPHERE to 1), consumed)
    }

    @Test
    fun crackle_incrementsEnergy() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("currentPortalEnergy", 7)
        assertTrue(
            PortalRequest.parseResponse(
                url = "campground.php?action=overpowerelvibratoportal",
                html = "There is a deafening crackle of energy as it sinks",
                preferences = prefs,
            ),
        )
        assertEquals(17, prefs.getInt("currentPortalEnergy", 0))
    }
}
