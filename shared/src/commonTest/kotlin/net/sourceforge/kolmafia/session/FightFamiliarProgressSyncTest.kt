package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class FightFamiliarProgressSyncTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
    }

    @Test
    fun hareChargeAndAdv() {
        FightFamiliarProgressSync.applyHare("familiar hops around", prefs)
        assertEquals(1, prefs.getInt("_hareCharge", 0))
        FightFamiliarProgressSync.applyHare("pulls an oversized pocketwatch", prefs)
        assertEquals(0, prefs.getInt("_hareCharge", -1))
        assertEquals(1, prefs.getInt("_hareAdv", 0))
    }

    @Test
    fun gibbererUnderwaterDoubleCharge() {
        FightFamiliarProgressSync.applyGibberer("mutters", prefs, underwater = true)
        assertEquals(2, prefs.getInt("_gibbererCharge", 0))
        FightFamiliarProgressSync.applyGibberer(
            "you feel time slow down",
            prefs,
            underwater = false,
        )
        assertEquals(1, prefs.getInt("_gibbererAdv", 0))
        assertEquals(0, prefs.getInt("_gibbererCharge", -1))
    }

    @Test
    fun candleAndFireProgressOnWin() {
        assertTrue(
            FightFamiliarProgressSync.apply(
                html = "won",
                preferences = prefs,
                familiarId = FightFamiliarProgressSync.FAMILIAR_CANDLE,
                won = true,
            ),
        )
        assertEquals(1, prefs.getInt("optimisticCandleProgress", 0))
        FightFamiliarProgressSync.apply(
            html = "won",
            preferences = prefs,
            familiarId = FightFamiliarProgressSync.FAMILIAR_GARBAGE_FIRE,
            won = true,
        )
        assertEquals(1, prefs.getInt("garbageFireProgress", 0))
    }

    @Test
    fun vintnerIncrementsToCap() {
        prefs.setInt("vintnerCharge", 12)
        FightFamiliarProgressSync.applyVintner("polite nod", prefs)
        assertEquals(13, prefs.getInt("vintnerCharge", 0))
        FightFamiliarProgressSync.applyVintner("clears his throat", prefs)
        assertEquals(13, prefs.getInt("vintnerCharge", 0))
    }

    @Test
    fun catBurglarCharge() {
        FightFamiliarProgressSync.applyCatBurglar(
            "takes note of any security cameras in the area",
            prefs,
        )
        assertEquals(1, prefs.getInt("_catBurglarCharge", 0))
    }

    @Test
    fun robortenderDrop() {
        FightFamiliarProgressSync.applyRobortender(
            "Allow Me To Recommend A Local Specialty",
            prefs,
        )
        assertEquals(1, prefs.getInt("_roboDrops", 0))
    }

    @Test
    fun grinderPieStuffing() {
        FightFamiliarProgressSync.applyGrinder(
            "harvests a few choice bits for his grinder",
            prefs,
        )
        assertEquals(1, prefs.getInt("_piePartsCount", 0))
        assertEquals("boss", prefs.getString("pieStuffing", ""))
    }
}
