package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LightsOutManagerTest {

    private fun prefs(block: MapSettings.() -> Unit = {}) =
        Preferences(MapSettings().apply(block))

    @Test
    fun lightsOutNow_trueOnMultipleOf37() {
        assertTrue(LightsOutManager.lightsOutNow(37, lastLightsOutTurn = 0))
        assertFalse(LightsOutManager.lightsOutNow(37, lastLightsOutTurn = 37))
        assertFalse(LightsOutManager.lightsOutNow(36, lastLightsOutTurn = 0))
    }

    @Test
    fun checkCounter_requiresTrackingAndRooms() {
        val off = prefs()
        assertFalse(LightsOutManager.checkCounter(off, 1))

        val on = prefs {
            putBoolean(LightsOutManager.TRACK_PREF, true)
            putString(LightsOutManager.NEXT_ELIZABETH, "The Haunted Bedroom")
        }
        assertTrue(LightsOutManager.checkCounter(on, 5))
        assertTrue(TurnCounter.isCounting(on, LightsOutManager.COUNTER_LABEL, 5))
        assertFalse(LightsOutManager.checkCounter(on, 5)) // already counting
    }

    @Test
    fun report_listsRooms() {
        val p = prefs {
            putString(LightsOutManager.NEXT_ELIZABETH, "none")
            putString(LightsOutManager.NEXT_STEPHEN, "The Haunted Library")
        }
        val lines = LightsOutManager.report(p)
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("defeated Elizabeth"))
        assertTrue(lines[1].contains("Haunted Library"))
    }
}
