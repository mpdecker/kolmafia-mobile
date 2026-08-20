package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportSync

class GameRuntimeLibraryAshP582Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun coldAdventure_setsTodayFlag() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            AirportSync.syncFromVisit(
                html = "You arrive at the Ice Hotel.",
                url = "adventure.php?snarfblat=455",
                prefs = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_coldAirportToday", false))
    }

    @Test
    fun hotPlace_setsTodayFlag() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            AirportSync.syncFromVisit(
                html = "Hot airport zone",
                url = "place.php?whichplace=airport_hot",
                prefs = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_hotAirportToday", false))
    }

    @Test
    fun alwaysGate_skipsTodayFlag() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("spookyAirportAlways", true)
        assertFalse(
            AirportSync.syncFromVisit(
                html = "ok",
                url = "place.php?whichplace=airport_spooky",
                prefs = prefs,
            ) && prefs.getBoolean("_spookyAirportToday", false),
        )
        assertFalse(prefs.getBoolean("_spookyAirportToday", false))
    }

    @Test
    fun blocked_doesNotSetFlag() {
        val prefs = Preferences(MapSettings())
        AirportSync.syncFromVisit(
            html = "You don't know where that is.",
            url = "adventure.php?snarfblat=442",
            prefs = prefs,
        )
        assertFalse(prefs.getBoolean("_stenchAirportToday", false))
    }

    @Test
    fun hubMap_detectsSleazeLink() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            AirportSync.syncFromVisit(
                html = """<a href="place.php?whichplace=airport_sleaze">Sleaze</a>""",
                url = "place.php?whichplace=airport",
                prefs = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_sleazeAirportToday", false))
    }
}
