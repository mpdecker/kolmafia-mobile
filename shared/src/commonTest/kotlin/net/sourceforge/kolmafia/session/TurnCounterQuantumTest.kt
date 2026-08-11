package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class TurnCounterQuantumTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun isCounting_trueWhenActiveCounterExists() {
        val prefs = prefs()
        TurnCounter.startCounting(prefs, currentRun = 10, turns = 5, "Quantum Familiar loc=*", "x.gif")
        assertTrue(TurnCounter.isCounting(prefs, "Quantum Familiar", currentRun = 10))
    }

    @Test
    fun isCounting_range_matchesExpiringWindow() {
        val prefs = prefs()
        TurnCounter.startCounting(prefs, currentRun = 10, turns = 1, "Quantum Familiar loc=*", "x.gif")
        assertTrue(
            TurnCounter.isCounting(prefs, "Quantum Familiar", currentRun = 10, start = 0, stop = 1),
        )
        assertFalse(
            TurnCounter.isCounting(prefs, "Quantum Familiar", currentRun = 10, start = 2, stop = 3),
        )
    }

    @Test
    fun getCounterLabels_returnsMatchingParsedLabels() {
        val prefs = prefs()
        TurnCounter.startCounting(prefs, currentRun = 20, turns = 1, "Quantum Familiar loc=*", "x.gif")
        val labels = TurnCounter.getCounterLabels(
            preferences = prefs,
            label = "Quantum Familiar",
            currentRun = 20,
            minTurns = 0,
            maxTurns = 1,
        )
        assertEquals(listOf("Quantum Familiar"), labels)
    }
}
