package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class TurnCounterXxxiiiTest {

    // ── startCountingTemporary ────────────────────────────────────────────────

    @Test
    fun startCountingTemporary_appendsToPref() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCountingTemporary(prefs, 5, "Test Counter", "test.gif")
        val raw = prefs.getString(TurnCounter.TEMP_PREF_KEY, "")
        assertEquals("5:Test Counter:test.gif|", raw)
    }

    @Test
    fun startCountingTemporary_appendsMultiple() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCountingTemporary(prefs, 5, "A", "a.gif")
        TurnCounter.startCountingTemporary(prefs, 10, "B", "b.gif")
        val raw = prefs.getString(TurnCounter.TEMP_PREF_KEY, "")
        assertEquals("5:A:a.gif|10:B:b.gif|", raw)
    }

    // ── handleTemporaryCounters ──────────────────────────────────────────────

    @Test
    fun handleTemporaryCounters_startsCountersAndClearsPref() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCountingTemporary(prefs, 5, "Test", "test.gif")
        TurnCounter.handleTemporaryCounters(
            preferences = prefs,
            currentRun = 100,
            type = "Noncombat",
            encounter = "something",
            lastLocationHasWanderers = true,
        )
        assertEquals("", prefs.getString(TurnCounter.TEMP_PREF_KEY, ""))
        val counters = TurnCounter.load(prefs)
        assertEquals(1, counters.size)
        assertEquals(105, counters[0].absoluteTurn)
        assertEquals("Test", counters[0].parsedLabel())
    }

    @Test
    fun handleTemporaryCounters_skipsWhenNoWanderers() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCountingTemporary(prefs, 5, "Test", "test.gif")
        TurnCounter.handleTemporaryCounters(
            preferences = prefs,
            currentRun = 100,
            type = "Noncombat",
            encounter = "something",
            lastLocationHasWanderers = false,
        )
        assertTrue(prefs.getString(TurnCounter.TEMP_PREF_KEY, "").isNotEmpty())
        assertTrue(TurnCounter.load(prefs).isEmpty())
    }

    @Test
    fun handleTemporaryCounters_skipsNoWanderCombat() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCountingTemporary(prefs, 5, "Test", "test.gif")
        TurnCounter.handleTemporaryCounters(
            preferences = prefs,
            currentRun = 100,
            type = "Combat",
            encounter = "no-wander monster",
            lastLocationHasWanderers = true,
            isNoWanderMonster = { it == "no-wander monster" },
        )
        assertTrue(prefs.getString(TurnCounter.TEMP_PREF_KEY, "").isNotEmpty())
        assertTrue(TurnCounter.load(prefs).isEmpty())
    }

    @Test
    fun handleTemporaryCounters_startsOnCombatIfWanderer() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCountingTemporary(prefs, 3, "X", "x.gif")
        TurnCounter.handleTemporaryCounters(
            preferences = prefs,
            currentRun = 50,
            type = "Combat",
            encounter = "regular monster",
            lastLocationHasWanderers = true,
            isNoWanderMonster = { false },
        )
        assertEquals("", prefs.getString(TurnCounter.TEMP_PREF_KEY, ""))
        assertEquals(1, TurnCounter.load(prefs).size)
    }

    // ── getExpiredCounter ────────────────────────────────────────────────────

    @Test
    fun getExpiredCounter_returnsExpiredCounter() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 100, 5, "My Counter", "icon.gif")
        val result = TurnCounter.getExpiredCounter(
            preferences = prefs,
            currentRun = 105,
            turnsUsed = 1,
            adventureId = "123",
            informational = false,
        )
        assertNotNull(result)
        assertEquals("My Counter", result.parsedLabel())
    }

    @Test
    fun getExpiredCounter_returnsNullWhenNotExpired() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 100, 10, "Future Counter", "icon.gif")
        val result = TurnCounter.getExpiredCounter(
            preferences = prefs,
            currentRun = 105,
            turnsUsed = 1,
            adventureId = "123",
            informational = false,
        )
        assertNull(result)
    }

    @Test
    fun getExpiredCounter_returnsNullForZeroTurnsUsed() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 100, 0, "Now", "x.gif")
        val result = TurnCounter.getExpiredCounter(
            preferences = prefs,
            currentRun = 100,
            turnsUsed = 0,
            adventureId = "",
            informational = false,
        )
        assertNull(result)
    }

    @Test
    fun getExpiredCounter_respectsLastWarned() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 100, 5, "Counter", "icon.gif")
        val first = TurnCounter.getExpiredCounter(prefs, 105, 1, "123", false)
        assertNotNull(first)
        // Second call at same turn should skip (lastWarned = currentRun)
        val second = TurnCounter.getExpiredCounter(prefs, 105, 1, "123", false)
        assertNull(second)
    }

    @Test
    fun getExpiredCounter_informational_defersUntilActualExpiry() {
        val prefs = Preferences(MapSettings())
        // Counter at turn 110, exempt via loc=*
        TurnCounter.startCounting(prefs, 100, 10, "Info Counter loc=*", "info.gif")
        // currentRun=108 + turnsUsed=3 => currentTurns=110, but counter.absoluteTurn(110) > currentRun(108)
        val result = TurnCounter.getExpiredCounter(prefs, 108, 3, "123", informational = true)
        assertNull(result)
        // At turn 110 (exact expiry), informational should fire
        val result2 = TurnCounter.getExpiredCounter(prefs, 110, 1, "123", informational = true)
        assertNotNull(result2)
    }

    // ── getUnexpiredCounters ─────────────────────────────────────────────────

    @Test
    fun getUnexpiredCounters_formatsCorrectly() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 100, 5, "A loc=*", "a.gif")
        TurnCounter.startCounting(prefs, 100, 10, "B", "b.gif")
        val result = TurnCounter.getUnexpiredCounters(prefs, 102)
        assertTrue(result.contains("A (3)"))
        assertTrue(result.contains("B (8)"))
    }

    @Test
    fun getUnexpiredCounters_skipsPastCounters() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 100, 2, "Old", "old.gif")
        TurnCounter.startCounting(prefs, 100, 10, "Future", "f.gif")
        val result = TurnCounter.getUnexpiredCounters(prefs, 105)
        assertTrue(result.contains("Future (5)"))
        assertTrue(!result.contains("Old"))
    }

    @Test
    fun getUnexpiredCounters_emptyWhenNoCounters() {
        val prefs = Preferences(MapSettings())
        assertEquals("", TurnCounter.getUnexpiredCounters(prefs, 100))
    }

    // ── Entry.isWander ───────────────────────────────────────────────────────

    @Test
    fun entry_isWander_trueForWindowLabel() {
        val entry = TurnCounter.Entry(100, "Romantic Monster window begin loc=*", "lparen.gif")
        assertTrue(entry.isWander)
    }

    @Test
    fun entry_isWander_trueForTypeWander() {
        val entry = TurnCounter.Entry(100, "Test type=wander", "x.gif")
        assertTrue(entry.isWander)
    }

    @Test
    fun entry_isWander_falseForNormalCounter() {
        val entry = TurnCounter.Entry(100, "Fortune Cookie", "fortune.gif")
        assertTrue(!entry.isWander)
    }

    // ── Entry.lastWarned ─────────────────────────────────────────────────────

    @Test
    fun entry_lastWarned_defaultsMinusOne() {
        val entry = TurnCounter.Entry(100, "Test", "x.gif")
        assertEquals(-1, entry.lastWarned)
    }
}
