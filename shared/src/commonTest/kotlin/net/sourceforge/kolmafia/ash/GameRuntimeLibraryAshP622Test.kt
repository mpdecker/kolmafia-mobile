package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ToppingPeakNcSync

class GameRuntimeLibraryAshP622Test {

    @Test
    fun revision_phase623() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun abooPeakTitle_lightsPyre() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("booPeakProgress", 42)
        assertTrue(
            ToppingPeakNcSync.applyFromAdventure(
                url = "adventure.php?snarfblat=296",
                html = "Come On Ghosty, Light My Pyre",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("booPeakLit"))
        assertEquals(0, prefs.getInt("booPeakProgress"))
    }

    @Test
    fun oilPeakTitle_lightsPeak() {
        val prefs = Preferences(MapSettings())
        prefs.setString("oilPeakProgress", "310.66")
        assertTrue(
            ToppingPeakNcSync.applyFromAdventure(
                url = "adventure.php?snarfblat=298",
                html = "Unimpressed with Pressure",
                preferences = prefs,
                adventureId = "298",
            ),
        )
        assertTrue(prefs.getBoolean("oilPeakLit"))
        assertEquals("0", prefs.getString("oilPeakProgress"))
    }

    @Test
    fun wrongAdventure_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            ToppingPeakNcSync.applyFromAdventure(
                url = "adventure.php?snarfblat=81",
                html = "Come On Ghosty, Light My Pyre",
                preferences = prefs,
            ),
        )
        assertFalse(prefs.getBoolean("booPeakLit", false))
    }
}
