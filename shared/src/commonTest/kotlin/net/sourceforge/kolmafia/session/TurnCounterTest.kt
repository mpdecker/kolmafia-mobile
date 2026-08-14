package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    @Test
    fun removeExpired_dropsPastCounters() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, currentRun = 10, turns = 5, "Soon", "a.gif")
        TurnCounter.startCounting(prefs, currentRun = 10, turns = 20, "Later", "b.gif")
        TurnCounter.removeExpired(prefs, currentRun = 15)
        val raw = prefs.getString(TurnCounter.PREF_KEY, "")
        assertTrue(raw.contains("Later"))
        assertTrue(!raw.contains("Soon"))
    }

    @Test
    fun findByLabel_andTurnsRemaining() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startNemesisAssassinUnlock(prefs, currentRun = 100)
        val entry = TurnCounter.findByLabel(prefs, "Nemesis Assassin window begin")
        assertNotNull(entry)
        assertEquals(5, TurnCounter.turnsRemaining(entry, currentRun = 100))
        assertEquals(0, TurnCounter.turnsRemaining(entry, currentRun = 105))
    }

    @Test
    fun formatRelayCounters_listsActiveCounters() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 50, 10, "Test Counter loc=*", "x.gif")
        val text = TurnCounter.formatRelayCounters(prefs, currentRun = 55)
        assertTrue(text.contains("Test Counter"))
        assertTrue(text.contains("5 turns"))
    }

    @Test
    fun addWarning_stripsLocStar() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 0, 5, "Foo loc=*", "watch.gif")
        TurnCounter.addWarning(prefs, "Foo")
        val raw = prefs.getString(TurnCounter.PREF_KEY, "")
        assertTrue(raw.contains("Foo"), raw)
        assertTrue(!raw.contains("loc=*"), raw)
    }

    @Test
    fun removeWarning_appendsLocStar() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 0, 5, "Foo", "watch.gif")
        TurnCounter.removeWarning(prefs, "Foo")
        val raw = prefs.getString(TurnCounter.PREF_KEY, "")
        assertTrue(raw.contains("Foo loc=*"), raw)
    }

    @Test
    fun stopWanderingMonsterWindows_removesKnownLabels() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 0, 10, "Romantic Monster window begin loc=*", "a.gif")
        TurnCounter.startCounting(prefs, 0, 20, "Holiday Monster window end loc=* type=wander", "b.gif")
        TurnCounter.startCounting(prefs, 0, 5, "Test Counter loc=*", "c.gif")
        val removed = TurnCounter.stopWanderingMonsterWindows(prefs)
        assertEquals(2, removed)
        val raw = prefs.getString(TurnCounter.PREF_KEY, "")
        assertTrue(!raw.contains("Romantic Monster"))
        assertTrue(!raw.contains("Holiday Monster"))
        assertTrue(raw.contains("Test Counter"))
    }

    @Test
    fun resetMayonnaiseWindowsForRun_leavesNonMayoCountersUnchanged() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, currentRun = 100, turns = 10, "Test Counter loc=*", "x.gif")
        val adjusted = TurnCounter.resetMayonnaiseWindowsForRun(prefs, currentRun = 100)
        assertEquals(0, adjusted)
        assertEquals(110, TurnCounter.findByLabel(prefs, "Test Counter")?.absoluteTurn)
    }

    @Test
    fun resetMayonnaiseWindowsForRun_rebasesRemainingMayoWindow() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(
            prefs,
            currentRun = 100,
            turns = 5,
            "Mmmmmmayonnaise window 1 loc=*",
            "mayo.gif",
        )
        val adjusted = TurnCounter.resetMayonnaiseWindowsForRun(prefs, currentRun = 102)
        assertEquals(1, adjusted)
        val entry = TurnCounter.findByLabel(prefs, "Mmmmmmayonnaise window 1")
        assertNotNull(entry)
        assertEquals(105, entry.absoluteTurn)
        assertEquals(3, TurnCounter.turnsRemaining(entry, currentRun = 102))
    }

    @Test
    fun resetMayonnaiseWindowsForRun_clampsExpiredMayoToZeroRemaining() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(
            prefs,
            currentRun = 100,
            turns = 5,
            "Mmmmmmayonnaise window 2 loc=*",
            "mayo.gif",
        )
        TurnCounter.resetMayonnaiseWindowsForRun(prefs, currentRun = 110)
        val entry = TurnCounter.findByLabel(prefs, "Mmmmmmayonnaise window 2")
        assertNotNull(entry)
        assertEquals(110, entry.absoluteTurn)
        assertEquals(0, TurnCounter.turnsRemaining(entry, currentRun = 110))
    }
}
