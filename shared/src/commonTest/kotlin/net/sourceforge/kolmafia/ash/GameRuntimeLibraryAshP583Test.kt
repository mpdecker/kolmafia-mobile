package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportSync

class GameRuntimeLibraryAshP583Test {

    @Test
    fun shawarmaShop_unlocks() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            AirportSync.applyBunker(
                url = "place.php?whichplace=airport_spooky_bunker",
                html = """<a href="shop.php?whichshop=si_shop1">SHAWARMA</a>""",
                prefs = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("SHAWARMAInitiativeUnlocked", false))
    }

    @Test
    fun canteenLocked_clearsUnlock() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("canteenUnlocked", true)
        assertTrue(
            AirportSync.applyBunker(
                url = "place.php?whichplace=airport_spooky_bunker",
                html = """<a href="place.php?whichplace=airport_spooky_bunker&action=si_shop2locked">locked</a>""",
                prefs = prefs,
            ),
        )
        assertFalse(prefs.getBoolean("canteenUnlocked", true))
    }

    @Test
    fun keycard_unlocksArmoryAndConsumes() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Int>()
        assertTrue(
            AirportSync.applyBunker(
                url = "place.php?whichplace=airport_spooky_bunker&action=si_shop3locked",
                html = "You insert the keycard and the door slides open",
                prefs = prefs,
                consumeItem = { consumed.add(it) },
            ),
        )
        assertTrue(prefs.getBoolean("armoryUnlocked", false))
        assertEquals(listOf(AirportSync.ARMORY_KEYCARD), consumed)
    }

    @Test
    fun visitHook_bunkerThroughSyncFromVisit() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            AirportSync.syncFromVisit(
                html = """<a href="shop.php?whichshop=si_shop2">Canteen</a>""",
                url = "place.php?whichplace=airport_spooky_bunker",
                prefs = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("canteenUnlocked", false))
        assertTrue(prefs.getBoolean("_spookyAirportToday", false))
    }
}
