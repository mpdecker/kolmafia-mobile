package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ElVibratoSync

class GameRuntimeLibraryAshP593Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun adventure_decrementsEnergyAndUpdatesTrapezoid() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("currentPortalEnergy", 5)
        assertTrue(ElVibratoSync.applyFromAdventure("164", prefs))
        assertEquals(4, prefs.getInt("currentPortalEnergy", -1))
        assertEquals(4, CampgroundInventorySync.load(prefs)[ElVibratoSync.TRAPEZOID])
    }

    @Test
    fun adventure_floorsAtZero() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("currentPortalEnergy", 0)
        assertTrue(ElVibratoSync.applyFromAdventure("164", prefs))
        assertEquals(0, prefs.getInt("currentPortalEnergy", -1))
    }

    @Test
    fun portal1_chargesFromZeroTo20() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            ElVibratoSync.applyFromCampground("""<img src="portal1.gif">""", prefs),
        )
        assertEquals(20, prefs.getInt("currentPortalEnergy", 0))
        assertEquals(20, CampgroundInventorySync.load(prefs)[ElVibratoSync.TRAPEZOID])
    }

    @Test
    fun portal2_setsEnergyZero() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("currentPortalEnergy", 12)
        assertTrue(
            ElVibratoSync.applyFromCampground("""<img src="portal2.gif">""", prefs),
        )
        assertEquals(0, prefs.getInt("currentPortalEnergy", -1))
        assertEquals(0, CampgroundInventorySync.load(prefs)[ElVibratoSync.TRAPEZOID] ?: 0)
    }
}
