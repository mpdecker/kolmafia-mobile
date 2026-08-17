package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FantasyRealmSync

class GameRuntimeLibraryAshP595Test {

    @Test
    fun permanentUnlocks_fromInitCenter() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            FantasyRealmSync.applyFromFantasyPlace(
                url = "place.php?whichplace=realm_fantasy",
                html = """action=fr_initcenter snarfblat=503 snarfblat=507""",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_frToday", false))
        assertTrue(prefs.getBoolean("frMountainsUnlocked", false))
        assertTrue(prefs.getBoolean("frCemetaryUnlocked", false))
        assertEquals(false, prefs.getBoolean("frWoodUnlocked", true))
    }

    @Test
    fun temporaryUnlocks_setsAreasCsv() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("frAlways", true)
        assertTrue(
            FantasyRealmSync.applyFromFantasyPlace(
                url = "place.php?whichplace=realm_fantasy",
                html = """snarfblat=502 snarfblat=504""",
                preferences = prefs,
            ),
        )
        assertEquals(false, prefs.getBoolean("_frToday", false))
        assertEquals(
            "The Bandit Crossroads,The Mystic Wood,",
            prefs.getString("_frAreasUnlocked", ""),
        )
    }

    @Test
    fun monorail_setsFrToday() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            FantasyRealmSync.applyFromMonorail(
                url = "place.php?whichplace=monorail",
                html = "FantasyRealm ticket booth",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_frToday", false))
    }
}
