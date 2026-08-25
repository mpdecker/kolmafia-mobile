package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.track.TrackManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FightTrackSyncTest {

    @Test
    fun applyFromFight_olfactionPattern_tracksMonster() {
        val prefs = Preferences(MapSettings())
        val tracker = FightTrackSync.applyFromFight(
            html = "You carefully examine the ground around you and find a scent.",
            monsterName = "spooky vampire",
            preferences = prefs,
            currentTurn = 42,
        )
        assertEquals(TrackManager.Tracker.OLFACTION, tracker)
        val entries = TrackManager.loadEntries(prefs, TrackManager.PREF_TRACKED_MONSTERS)
        assertEquals(1, entries.size)
        assertEquals("spooky vampire", entries[0].tracked)
        assertEquals(42, entries[0].turnTracked)
    }

    @Test
    fun applyFromFight_noPattern_returnsNull() {
        val prefs = Preferences(MapSettings())
        val tracker = FightTrackSync.applyFromFight(
            html = "You win the fight!",
            monsterName = "goblin",
            preferences = prefs,
            currentTurn = 1,
        )
        assertEquals(null, tracker)
        assertEquals("", prefs.getString(TrackManager.PREF_TRACKED_MONSTERS, ""))
    }

    @Test
    fun applyFromFight_latte_tracks() {
        val prefs = Preferences(MapSettings())
        assertNotNull(
            FightTrackSync.applyFromFight(
                html = "You offer your opponent a latte.",
                monsterName = "Knob Goblin",
                preferences = prefs,
                currentTurn = 5,
            ),
        )
        assertEquals(
            TrackManager.Tracker.LATTE,
            TrackManager.loadEntries(prefs, TrackManager.PREF_TRACKED_MONSTERS).single().tracker,
        )
    }
}
