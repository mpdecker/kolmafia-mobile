package net.sourceforge.kolmafia.track

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackManagerCombatApiTest {

    @Test
    fun trackMonster_replacesPriorEntryForSameTracker() {
        val prefs = Preferences(MapSettings())
        TrackManager.trackMonster(prefs, "goblin", TrackManager.Tracker.OLFACTION, 10)
        TrackManager.trackMonster(prefs, "orc", TrackManager.Tracker.OLFACTION, 20)
        val entries = TrackManager.loadEntries(prefs, TrackManager.PREF_TRACKED_MONSTERS)
        assertEquals(1, entries.size)
        assertEquals("orc", entries[0].tracked)
        assertEquals(3, TrackManager.countCopies(prefs, "orc", 20))
        assertEquals(0, TrackManager.countCopies(prefs, "goblin", 20))
        assertTrue(TrackManager.isQueueIgnored(prefs, "orc", 20))
        assertFalse(TrackManager.isQueueIgnored(prefs, "goblin", 20))
    }

    @Test
    fun track_phylum_writesTrackedPhyla() {
        val prefs = Preferences(MapSettings())
        TrackManager.track(prefs, "beast", TrackManager.Tracker.RED_SNAPPER, 5)
        val entries = TrackManager.loadEntries(prefs, TrackManager.PREF_TRACKED_PHYLA)
        assertEquals(1, entries.size)
        assertEquals("beast", entries[0].tracked)
        assertEquals(2, entries[0].tracker.copies)
    }

    @Test
    fun countCopies_sumsMonsterTracks() {
        val prefs = Preferences(MapSettings())
        TrackManager.trackMonster(prefs, "dairy goat", TrackManager.Tracker.OLFACTION, 1)
        TrackManager.track(prefs, "beast", TrackManager.Tracker.A_BEASTLY_ODOR, 1)
        val copies = TrackManager.countCopies(prefs, "dairy goat", 1)
        // Olfaction alone is 3; if MonsterDatabase is loaded and dairy goat is beast, +2 phylum.
        assertTrue(copies == 3 || copies == 5, "expected 3 or 5 copies, got $copies")
        val phylumEntries = TrackManager.loadEntries(prefs, TrackManager.PREF_TRACKED_PHYLA)
        assertEquals(1, phylumEntries.size)
        assertEquals(2, phylumEntries.single().tracker.copies)
    }

    @Test
    fun resetAvatar_clearsAvatarTracks() {
        val prefs = Preferences(MapSettings())
        TrackManager.trackMonster(prefs, "a", TrackManager.Tracker.MAKE_FRIENDS, 1)
        TrackManager.trackMonster(prefs, "b", TrackManager.Tracker.OLFACTION, 1)
        val cleared = TrackManager.resetAvatar(prefs)
        assertEquals(1, cleared)
        assertTrue(TrackManager.trackedBy(prefs, "b").contains("Transcendent Olfaction"))
        assertTrue(TrackManager.trackedBy(prefs, "a").isEmpty())
    }
}
