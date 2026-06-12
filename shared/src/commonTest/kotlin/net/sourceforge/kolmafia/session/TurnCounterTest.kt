package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurnCounterTest {

    @Test
    fun startNemesisAssassinUnlock_writesRelayCounters() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startNemesisAssassinUnlock(prefs, currentRun = 100)
        val raw = prefs.getString(TurnCounter.PREF_KEY, "")
        assertTrue(raw.contains("Nemesis Assassin window begin"))
        assertTrue(raw.contains("Nemesis Assassin window end"))
        assertTrue(raw.contains("105:"))
        assertTrue(raw.contains("115:"))
    }

    @Test
    fun resetNemesisAssassinWindow_replacesCounters() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startNemesisAssassinUnlock(prefs, 10)
        TurnCounter.resetNemesisAssassinWindow(prefs, 200)
        val raw = prefs.getString(TurnCounter.PREF_KEY, "")
        assertTrue(raw.contains("235:"))
        assertTrue(raw.contains("250:"))
    }

    @Test
    fun stopCounting_removesByLabel() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 0, 5, "Test Counter loc=*", "foo.gif")
        TurnCounter.stopCounting(prefs, "Test Counter")
        assertEquals("", prefs.getString(TurnCounter.PREF_KEY, ""))
    }
}
