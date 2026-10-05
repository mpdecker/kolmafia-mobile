package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.TurnCounter

class StopForCountersTest {

    @Test
    fun check_abortsOnExpiredCounter() {
        val prefs = Preferences(MapSettings())
        TurnCounter.clearCounters(prefs)
        // absoluteTurn 10 with currentRun 10, turnsUsed 1 → expires at turn 10
        TurnCounter.startCounting(prefs, currentRun = 10, turns = 0, label = "Test Counter", image = "x.gif")

        val result = StopForCounters.check(
            preferences = prefs,
            currentRun = 10,
            turnsUsed = 1,
            adventureId = "100",
        )
        assertTrue(result.shouldStop)
        assertTrue(result.message.contains("Test Counter"))
    }

    @Test
    fun check_respectsDontStopPref() {
        val prefs = Preferences(MapSettings())
        TurnCounter.clearCounters(prefs)
        prefs.setBoolean(StopForCounters.DONT_STOP_PREF, true)
        TurnCounter.startCounting(prefs, currentRun = 5, turns = 0, label = "Ignored", image = "x.gif")

        val result = StopForCounters.check(
            preferences = prefs,
            currentRun = 5,
            turnsUsed = 1,
            adventureId = "100",
        )
        assertFalse(result.shouldStop)
    }

    @Test
    fun check_continuesWhenCounterScriptHandles() {
        val prefs = Preferences(MapSettings())
        TurnCounter.clearCounters(prefs)
        TurnCounter.startCounting(prefs, currentRun = 3, turns = 0, label = "Scripted", image = "x.gif")

        val result = StopForCounters.check(
            preferences = prefs,
            currentRun = 3,
            turnsUsed = 1,
            adventureId = "100",
            onCounter = { _, _ -> true },
        )
        assertFalse(result.shouldStop)
        assertEquals("", result.message)
    }
}
