package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.GingerbreadCitySync

class GameRuntimeLibraryAshP584Test {

    @Test
    fun placeVisit_setsTodayWhenNotAlwaysAvailable() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            GingerbreadCitySync.applyFromVisit(
                url = "place.php?whichplace=gingerbreadcity",
                html = "Welcome to Gingerbread City",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_gingerbreadCityToday", false))
    }

    @Test
    fun retailSnarfblat_unlocksRetail() {
        val prefs = Preferences(MapSettings())
        GingerbreadCitySync.applyFromVisit(
            url = "place.php?whichplace=gingerbreadcity",
            html = """<a href="adventure.php?snarfblat=480">Retail</a>""",
            preferences = prefs,
        )
        assertTrue(prefs.getBoolean("gingerRetailUnlocked", false))
    }

    @Test
    fun sewersAndClock_unlock() {
        val prefs = Preferences(MapSettings())
        GingerbreadCitySync.applyFromVisit(
            url = "place.php?whichplace=gingerbreadcity",
            html = """<a href="adventure.php?snarfblat=481">Sewers</a><img src="digitalclock.gif">""",
            preferences = prefs,
        )
        assertTrue(prefs.getBoolean("gingerSewersUnlocked", false))
        assertTrue(prefs.getBoolean("gingerAdvanceClockUnlocked", false))
    }

    @Test
    fun infrastructureFailure_incrementsTurns() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_gingerbreadCityTurns", 2)
        GingerbreadCitySync.applyFromVisit(
            url = "adventure.php?snarfblat=479",
            html = "Infrastructure Failure strikes again",
            preferences = prefs,
        )
        assertEquals(3, prefs.getInt("_gingerbreadCityTurns", 0))
    }

    @Test
    fun alwaysAvailable_skipsTodayFlag() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("gingerbreadCityAvailable", true)
        GingerbreadCitySync.applyFromVisit(
            url = "place.php?whichplace=gingerbreadcity",
            html = "Welcome",
            preferences = prefs,
        )
        assertFalse(prefs.getBoolean("_gingerbreadCityToday", false))
    }
}
