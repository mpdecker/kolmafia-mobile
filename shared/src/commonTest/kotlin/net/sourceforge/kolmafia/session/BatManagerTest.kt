package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class BatManagerTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        BatManager.resetForTest()
        prefs = Preferences(MapSettings())
    }

    @Test
    fun beginSetsTimeAndFunds() {
        BatManager.begin(prefs)
        assertEquals(600, prefs.getInt("batmanTimeLeft", 0))
        assertEquals(3, prefs.getInt("batmanFundsAvailable", 0))
        assertEquals(BatManager.BAT_CAVERN, prefs.getString("batmanZone", ""))
    }

    @Test
    fun endClearsState() {
        BatManager.begin(prefs)
        BatManager.end(prefs)
        assertEquals(0, prefs.getInt("batmanTimeLeft", -1))
        assertEquals(0, prefs.getInt("batmanFundsAvailable", -1))
        assertEquals("", prefs.getString("batmanUpgrades", "x"))
    }

    @Test
    fun suitUpgradeAppliesStatsAndFunds() {
        prefs.setInt("batmanFundsAvailable", 3)
        assertTrue(BatManager.batSuitUpgrade(1, prefs))
        assertTrue(BatManager.hasUpgrade("Hardened Knuckles"))
        assertEquals(2, BatManager.getStat("Bat-Punch Multiplier"))
        assertEquals(2, prefs.getInt("batmanFundsAvailable", 0))
    }

    @Test
    fun duplicateUpgradeRejected() {
        prefs.setString("batmanUpgrades", "Spotlight")
        prefs.setInt("batmanFundsAvailable", 5)
        BatManager.restoreUpgradesFromPref(prefs)
        assertFalse(BatManager.batSedanUpgrade(6, prefs))
        assertEquals(5, prefs.getInt("batmanFundsAvailable", 0))
    }

    @Test
    fun placeResponseSetsZone() {
        BatManager.parsePlaceResponse("place.php?whichplace=batman_downtown", prefs)
        assertEquals(BatManager.DOWNTOWN, BatManager.currentBatZone())
    }

    @Test
    fun wonFightTimeBandit() {
        BatManager.begin(prefs)
        BatManager.wonFight(
            "time bandit",
            "(+10 Bat-Minutes)",
            prefs,
        )
        assertEquals(610, prefs.getInt("batmanTimeLeft", 0))
    }

    @Test
    fun sedanChoiceSetsZone() {
        assertTrue(BatManager.parseBatSedan("", 2, prefs))
        assertEquals(BatManager.SLUMS, BatManager.currentBatZone())
    }
}
