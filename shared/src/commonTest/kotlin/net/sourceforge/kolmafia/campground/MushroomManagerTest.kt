package net.sourceforge.kolmafia.campground

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences

class MushroomManagerTest {

    @Test
    fun plantAndPick_updatePlotSquares() {
        val prefs = Preferences(MapSettings())
        assertEquals(true, MushroomManager.plantMushroom(1, "kb", prefs))
        assertEquals("kb", MushroomManager.squareShorthand(prefs, 0))
        assertEquals(true, prefs.getBoolean("_mushroomPlanted", false))
        assertEquals(true, MushroomManager.pickMushroom(1, prefs, pickSpores = true))
        assertEquals("__", MushroomManager.squareShorthand(prefs, 0))
        assertEquals(true, prefs.getBoolean("_mushroomPicked", false))
    }
}
