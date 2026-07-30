package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.DefaultsDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class RolloverCounterResetTest {

    @AfterTest
    fun tearDown() {
        DefaultsDatabase.resetForTest()
    }

    @Test
    fun shouldResetCounters_rolloverGap() {
        assertTrue(RolloverCounterReset.shouldResetCounters(10_000L, 5_000L))
        assertFalse(RolloverCounterReset.shouldResetCounters(5_000L, 5_000L))
        assertFalse(RolloverCounterReset.shouldResetCounters(0L, -1L))
        assertTrue(RolloverCounterReset.shouldResetCounters(3_700L, -1L))
    }

    @Test
    fun unusedHeistsFromCharge_matchesDesktopCosts() {
        assertEquals(0, RolloverCounterReset.unusedHeistsFromCharge(5))
        assertEquals(1, RolloverCounterReset.unusedHeistsFromCharge(10))
        assertEquals(2, RolloverCounterReset.unusedHeistsFromCharge(30))
        assertEquals(3, RolloverCounterReset.unusedHeistsFromCharge(70))
    }

    @Test
    fun catBurglar_carryover_addsUnusedHeists() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(RolloverCounterReset.CAT_BURGLAR_CHARGE, 30)
        prefs.setInt(RolloverCounterReset.CAT_BURGLAR_HEISTS_COMPLETE, 1)
        prefs.setInt(RolloverCounterReset.CAT_BURGLAR_BANK_HEISTS, 2)
        val delta = RolloverCounterReset.carryOverCatBurglarBankHeists(prefs)
        assertEquals(1, delta)
        assertEquals(3, prefs.getInt(RolloverCounterReset.CAT_BURGLAR_BANK_HEISTS, 0))
    }

    @Test
    fun catBurglar_noCharge_leavesBankUnchanged() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(RolloverCounterReset.CAT_BURGLAR_CHARGE, 5)
        prefs.setInt(RolloverCounterReset.CAT_BURGLAR_BANK_HEISTS, 4)
        val delta = RolloverCounterReset.carryOverCatBurglarBankHeists(prefs)
        assertEquals(0, delta)
        assertEquals(4, prefs.getInt(RolloverCounterReset.CAT_BURGLAR_BANK_HEISTS, 0))
    }

    @Test
    fun kolhs_resetsWhenNotSpiritedYesterday() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(RolloverCounterReset.KOLHS_TOTAL, 7)
        prefs.setBoolean(RolloverCounterReset.KOLHS_YESTERDAY, false)
        assertTrue(RolloverCounterReset.resetKolhsTotalSchoolSpirited(prefs))
        assertEquals(0, prefs.getInt(RolloverCounterReset.KOLHS_TOTAL, -1))
    }

    @Test
    fun kolhs_preservesWhenSpiritedYesterday() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(RolloverCounterReset.KOLHS_TOTAL, 7)
        prefs.setBoolean(RolloverCounterReset.KOLHS_YESTERDAY, true)
        assertFalse(RolloverCounterReset.resetKolhsTotalSchoolSpirited(prefs))
        assertEquals(7, prefs.getInt(RolloverCounterReset.KOLHS_TOTAL, -1))
    }

    @Test
    fun resetCounters_setsLastCounterDay() {
        val prefs = Preferences(MapSettings())
        RolloverCounterReset.resetCounters(prefs, rolloverTimestamp = 1_700_000_000L)
        assertEquals(1_700_000_000L, prefs.getLong(RolloverCounterReset.LAST_COUNTER_DAY, -1L))
    }

    @Test
    fun resetCounters_callsDailyResets() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.ParseSnapshot(
                entries = mapOf(
                    "ascensionsToday" to DefaultsDatabase.Entry(
                        DefaultsDatabase.Scope.USER,
                        "ascensionsToday",
                        "0",
                    ),
                    "potatoAlarmClockUsed" to DefaultsDatabase.Entry(
                        DefaultsDatabase.Scope.USER,
                        "potatoAlarmClockUsed",
                        "false",
                    ),
                ),
                resetOnAscension = emptySet(),
                resetOnFight = emptySet(),
                legacyDailies = emptySet(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("ascensionsToday", 3)
        prefs.setBoolean("_dailyPref", true)
        val summary = RolloverCounterReset.resetCounters(prefs, rolloverTimestamp = 1_700_000_000L)
        assertTrue(summary.perRolloverPrefsReset >= 1)
        assertTrue(summary.dailyPrefsReset >= 1)
        assertEquals(0, prefs.getInt("ascensionsToday", -1))
        assertFalse(prefs.hasKey("_dailyPref"))
    }
}
