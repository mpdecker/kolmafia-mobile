package net.sourceforge.kolmafia.track

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    // ── Tracker.isEffective ──────────────────────────────────────────────────

    @Test
    fun isEffective_olfaction_alwaysTrue() {
        assertTrue(TrackManager.Tracker.OLFACTION.isEffective(-1))
        assertTrue(TrackManager.Tracker.OLFACTION.isEffective(0))
        assertTrue(TrackManager.Tracker.OLFACTION.isEffective(173))
    }

    @Test
    fun isEffective_nosyNose_requiresFamiliar173() {
        assertTrue(TrackManager.Tracker.NOSY_NOSE.isEffective(173))
        assertFalse(TrackManager.Tracker.NOSY_NOSE.isEffective(0))
        assertFalse(TrackManager.Tracker.NOSY_NOSE.isEffective(275))
        assertFalse(TrackManager.Tracker.NOSY_NOSE.isEffective(-1))
    }

    @Test
    fun isEffective_redSnapper_requiresFamiliar275() {
        assertTrue(TrackManager.Tracker.RED_SNAPPER.isEffective(275))
        assertFalse(TrackManager.Tracker.RED_SNAPPER.isEffective(0))
        assertFalse(TrackManager.Tracker.RED_SNAPPER.isEffective(173))
        assertFalse(TrackManager.Tracker.RED_SNAPPER.isEffective(-1))
    }

    // ── countCopies / trackedBy / isQueueIgnored with isEffective ────────────

    @Test
    fun countCopies_nosyNose_excludedWhenWrongFamiliar() {
        val prefs = Preferences(MapSettings())
        TrackManager.track(prefs, "goblin", TrackManager.Tracker.NOSY_NOSE, currentTurn = 10)
        TrackManager.track(prefs, "goblin", TrackManager.Tracker.OLFACTION, currentTurn = 10)

        // With Nosy Nose active (173): both count
        val withNose = TrackManager.countCopies(prefs, "goblin", currentTurn = 20, currentFamiliarId = 173)
        assertEquals(4, withNose) // 1 (nosy) + 3 (olfaction)

        // With wrong familiar: only olfaction counts
        val withoutNose = TrackManager.countCopies(prefs, "goblin", currentTurn = 20, currentFamiliarId = 0)
        assertEquals(3, withoutNose) // only olfaction
    }

    @Test
    fun trackedBy_nosyNose_excludedWhenWrongFamiliar() {
        val prefs = Preferences(MapSettings())
        TrackManager.track(prefs, "goblin", TrackManager.Tracker.NOSY_NOSE, currentTurn = 10)
        TrackManager.track(prefs, "goblin", TrackManager.Tracker.GALLAPAGOS, currentTurn = 10)

        val with = TrackManager.trackedBy(prefs, "goblin", currentTurn = 20, currentFamiliarId = 173)
        assertEquals(2, with.size)

        val without = TrackManager.trackedBy(prefs, "goblin", currentTurn = 20, currentFamiliarId = 0)
        assertEquals(1, without.size)
        assertEquals("Gallapagosian Mating Call", without.first())
    }

    @Test
    fun isQueueIgnored_olfaction_effectiveRegardlessOfFamiliar() {
        val prefs = Preferences(MapSettings())
        TrackManager.track(prefs, "goblin", TrackManager.Tracker.OLFACTION, currentTurn = 10)

        assertTrue(TrackManager.isQueueIgnored(prefs, "goblin", currentTurn = 20, currentFamiliarId = 0))
        assertTrue(TrackManager.isQueueIgnored(prefs, "goblin", currentTurn = 20, currentFamiliarId = 173))
    }
}
