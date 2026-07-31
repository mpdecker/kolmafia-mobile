package net.sourceforge.kolmafia.track

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackManagerTest {

    @Test
    fun loadEntries_roundTripsKnownTrackPref() {
        val prefs = Preferences(MapSettings())
        prefs.setString(
            TrackManager.PREF_TRACKED_MONSTERS,
            "goblin:Gallapagosian Mating Call:100:boss:Make Friends:200",
        )

        val entries = TrackManager.loadEntries(prefs, TrackManager.PREF_TRACKED_MONSTERS)

        assertEquals(2, entries.size)
        assertEquals("goblin", entries[0].tracked)
        assertEquals(TrackManager.Tracker.GALLAPAGOS, entries[0].tracker)
        assertEquals(100, entries[0].turnTracked)
        assertEquals("boss", entries[1].tracked)
        assertEquals(TrackManager.Tracker.MAKE_FRIENDS, entries[1].tracker)

        TrackManager.saveEntries(prefs, TrackManager.PREF_TRACKED_MONSTERS, entries)
        assertEquals(
            "goblin:Gallapagosian Mating Call:100:boss:Make Friends:200",
            prefs.getString(TrackManager.PREF_TRACKED_MONSTERS, ""),
        )
    }

    @Test
    fun resetRollover_removesRolloverTurnRolloverAndAvatarRolloverTracks() {
        val prefs = Preferences(MapSettings())
        prefs.setString(
            TrackManager.PREF_TRACKED_MONSTERS,
            listOf(
                "goblin:Gallapagosian Mating Call:10",
                "orc:Offer Latte to Opponent:20",
                "troll:Staff of the Cream of the Cream:30",
                "boss:Make Friends:40",
                "imp:Be Superficially interested:50",
                "bat:Transcendent Olfaction:60",
            ).joinToString(":"),
        )
        prefs.setString(
            TrackManager.PREF_TRACKED_PHYLA,
            "beast:Red-Nosed Snapper:70:undead:Ew, The Humanity:80",
        )

        val cleared = TrackManager.resetRollover(prefs)

        assertEquals(3, cleared)
        assertEquals(
            listOf(
                "boss:Make Friends:40",
                "imp:Be Superficially interested:50",
                "bat:Transcendent Olfaction:60",
            ).joinToString(":"),
            prefs.getString(TrackManager.PREF_TRACKED_MONSTERS, ""),
        )
        assertEquals(
            "beast:Red-Nosed Snapper:70:undead:Ew, The Humanity:80",
            prefs.getString(TrackManager.PREF_TRACKED_PHYLA, ""),
        )
    }

    @Test
    fun resetRollover_clearsAllRolloverResetTrackerTypes() {
        val prefs = Preferences(MapSettings())
        prefs.setString(
            TrackManager.PREF_TRACKED_MONSTERS,
            listOf(
                "a:McHugeLarge Slash:1",
                "b:Meat Cute:2",
                "c:Try to Remember:3",
                "d:Baseball Diamond:4",
                "e:prank Crimbo card:5",
                "f:trick coin:6",
            ).joinToString(":"),
        )

        val cleared = TrackManager.resetRollover(prefs)

        assertEquals(6, cleared)
        assertTrue(prefs.getString(TrackManager.PREF_TRACKED_MONSTERS, "").isBlank())
    }

    @Test
    fun resetRollover_preservesUnknownTrackerNames() {
        val prefs = Preferences(MapSettings())
        prefs.setString(
            TrackManager.PREF_TRACKED_MONSTERS,
            "goblin:Unknown Tracker Foo:10:boss:Make Friends:20",
        )

        TrackManager.resetRollover(prefs)

        assertEquals(
            "goblin:Unknown Tracker Foo:10:boss:Make Friends:20",
            prefs.getString(TrackManager.PREF_TRACKED_MONSTERS, ""),
        )
    }
}
