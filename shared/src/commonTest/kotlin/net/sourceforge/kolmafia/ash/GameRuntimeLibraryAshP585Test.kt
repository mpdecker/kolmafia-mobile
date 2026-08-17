package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.SpacegateVisitSync

class GameRuntimeLibraryAshP585Test {

    @Test
    fun placeVisit_setsToday() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SpacegateVisitSync.applyFromVisit(
                url = "place.php?whichplace=spacegate",
                html = "Spacegate Facility",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_spacegateToday", false))
    }

    @Test
    fun adventure494_setsToday() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SpacegateVisitSync.applyFromVisit(
                url = "adventure.php?snarfblat=494",
                html = "You arrive through the gate",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_spacegateToday", false))
    }

    @Test
    fun always_skipsTodayFlag() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("spacegateAlways", true)
        assertFalse(
            SpacegateVisitSync.applyFromVisit(
                url = "place.php?whichplace=spacegate",
                html = "Spacegate Facility",
                preferences = prefs,
            ),
        )
        assertFalse(prefs.getBoolean("_spacegateToday", false))
    }
}
