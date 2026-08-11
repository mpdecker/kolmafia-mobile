package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandWarVisitSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun applyFromBigIslandVisit_setsWarProgressStarted() {
        val prefs = prefs()
        prefs.setString("warProgress", "unstarted")
        assertTrue(IslandWarVisitSync.applyFromBigIslandVisit("<html></html>", prefs))
        assertEquals("started", prefs.getString("warProgress", ""))
    }

    @Test
    fun applyFromBigIslandVisit_noMapPattern_leavesCountersUnchanged() {
        val prefs = prefs()
        prefs.setInt("fratboysDefeated", 42)
        prefs.setInt("hippiesDefeated", 57)
        IslandWarVisitSync.applyFromBigIslandVisit("<html>no battlefield map</html>", prefs)
        assertEquals(42, prefs.getInt("fratboysDefeated", 0))
        assertEquals(57, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyFromBigIslandVisit_clampsFratboysUpToImageMin() {
        val prefs = prefs()
        prefs.setInt("fratboysDefeated", 10)
        // Image 4 → min=28, max=39
        IslandWarVisitSync.applyFromBigIslandVisit("<img src=bfleft4><img src=bfright0>", prefs)
        assertEquals(28, prefs.getInt("fratboysDefeated", 0))
    }

    @Test
    fun applyFromBigIslandVisit_clampsHippiesDownToImageMax() {
        val prefs = prefs()
        prefs.setInt("hippiesDefeated", 200)
        // Image 10 → min=114, max=131
        IslandWarVisitSync.applyFromBigIslandVisit("<img src=bfleft0><img src=bfright10>", prefs)
        assertEquals(131, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyFromBigIslandVisit_inRangeCounterUnchanged() {
        val prefs = prefs()
        prefs.setInt("fratboysDefeated", 35)
        prefs.setInt("hippiesDefeated", 120)
        IslandWarVisitSync.applyFromBigIslandVisit("<img src=bfleft4><img src=bfright10>", prefs)
        assertEquals(35, prefs.getInt("fratboysDefeated", 0))
        assertEquals(120, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyFromBigIslandVisit_image32ClampsBothTo1000() {
        val prefs = prefs()
        prefs.setInt("fratboysDefeated", 500)
        prefs.setInt("hippiesDefeated", 800)
        IslandWarVisitSync.applyFromBigIslandVisit("<img src=bfleft32><img src=bfright32>", prefs)
        assertEquals(1000, prefs.getInt("fratboysDefeated", 0))
        assertEquals(1000, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyFromBigIslandVisit_bothSidesClampedIndependently() {
        val prefs = prefs()
        prefs.setInt("fratboysDefeated", 5)
        prefs.setInt("hippiesDefeated", 250)
        // Image 5 → frat min=40; image 11 → hippy max=151
        IslandWarVisitSync.applyFromBigIslandVisit("<img src=bfleft5><img src=bfright11>", prefs)
        assertEquals(40, prefs.getInt("fratboysDefeated", 0))
        assertEquals(151, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun imageRange_image4_returns28To39() {
        assertEquals(28 to 39, IslandWarVisitSync.imageRange(4))
    }

    @Test
    fun imageRange_outOfRange_returnsNull() {
        assertEquals(null, IslandWarVisitSync.imageRange(-1))
        assertEquals(null, IslandWarVisitSync.imageRange(33))
    }

    @Test
    fun applyFromBigIslandVisit_nullPreferences_returnsFalse() {
        assertFalse(IslandWarVisitSync.applyFromBigIslandVisit("<html></html>", null))
    }
}
