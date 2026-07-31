package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.track.TrackManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RolloverCounterResetTest {

    @Test
    fun resetCounters_adjustsMayonnaiseWindows() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(
            prefs,
            currentRun = 200,
            turns = 8,
            "Mmmmmmayonnaise window 3 loc=*",
            "mayo.gif",
        )
        val summary = RolloverCounterReset.resetCounters(
            preferences = prefs,
            rolloverTimestamp = 1_700_000_000L,
            currentRun = 203,
        )
        assertEquals(1, summary.mayonnaiseCountersAdjusted)
        val entry = TurnCounter.findByLabel(prefs, "Mmmmmmayonnaise window 3")
        assertEquals(208, entry?.absoluteTurn)
    }

    @Test
    fun resetCounters_clearsBanishRolloverViaManager() {
        val prefs = Preferences(MapSettings())
        val banishManager = BanishManager(prefs)
        banishManager.banishMonster("Goblin", Banisher.BEANCANNON, currentTurn = 50)
        banishManager.banishMonster("Boss", Banisher.ICE_HOUSE, currentTurn = 50)

        val summary = RolloverCounterReset.resetCounters(
            preferences = prefs,
            rolloverTimestamp = 1_700_000_000L,
            currentRun = 100,
            banishManager = banishManager,
        )

        assertEquals(1, summary.banishRolloverCleared)
        assertFalse(banishManager.isBanished("Goblin", currentTurn = 100))
        assertTrue(banishManager.isBanished("Boss", currentTurn = 100))
    }

    @Test
    fun resetCounters_clearsTrackRolloverPrefs() {
        val prefs = Preferences(MapSettings())
        prefs.setString(
            TrackManager.PREF_TRACKED_MONSTERS,
            "goblin:Gallapagosian Mating Call:10:boss:Make Friends:20",
        )
        prefs.setString(
            TrackManager.PREF_TRACKED_PHYLA,
            "beast:Baseball Diamond:30:plant:Red-Nosed Snapper:40",
        )

        val summary = RolloverCounterReset.resetCounters(
            preferences = prefs,
            rolloverTimestamp = 1_700_000_000L,
            currentRun = 100,
        )

        assertEquals(2, summary.trackRolloverCleared)
        assertEquals("boss:Make Friends:20", prefs.getString(TrackManager.PREF_TRACKED_MONSTERS, ""))
        assertEquals("plant:Red-Nosed Snapper:40", prefs.getString(TrackManager.PREF_TRACKED_PHYLA, ""))
    }

    @Test
    fun carryOverCatBurglarBankHeists_accumulatesUnusedHeists() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(RolloverCounterReset.CAT_BURGLAR_CHARGE, 30)
        prefs.setInt(RolloverCounterReset.CAT_BURGLAR_HEISTS_COMPLETE, 0)
        prefs.setInt(RolloverCounterReset.CAT_BURGLAR_BANK_HEISTS, 0)

        val delta = RolloverCounterReset.carryOverCatBurglarBankHeists(prefs)

        assertEquals(2, delta)
        assertEquals(2, prefs.getInt(RolloverCounterReset.CAT_BURGLAR_BANK_HEISTS, 0))
    }

    @Test
    fun resetKolhsTotalSchoolSpirited_clearsWhenNotSpiritedYesterday() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(RolloverCounterReset.KOLHS_TOTAL, 42)
        prefs.setBoolean(RolloverCounterReset.KOLHS_YESTERDAY, false)

        assertTrue(RolloverCounterReset.resetKolhsTotalSchoolSpirited(prefs))
        assertEquals(0, prefs.getInt(RolloverCounterReset.KOLHS_TOTAL, -1))
    }

    @Test
    fun shouldResetCounters_trueWhenRolloverGapExceedsOneHour() {
        assertTrue(
            RolloverCounterReset.shouldResetCounters(
                rolloverTimestamp = 1_700_010_000L,
                lastCounterDay = 1_700_000_000L,
            ),
        )
    }
}
