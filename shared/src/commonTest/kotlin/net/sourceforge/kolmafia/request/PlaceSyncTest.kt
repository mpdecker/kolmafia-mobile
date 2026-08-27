package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class PlaceSyncTest {

    @Test
    fun chateau_setsAvailableAndFurniture() {
        val prefs = Preferences(MapSettings())
        PlaceSync.parseResponse(
            url = "place.php?whichplace=chateau",
            html = """
                <b>ceiling fan</b>
                <b>fancy french bed</b>
                Painting of a angry cow.
            """.trimIndent(),
            preferences = prefs,
        )
        assertEquals(true, prefs.getBoolean("chateauAvailable", false))
        assertEquals(true, prefs.getBoolean("chateauInstalled", false))
        assertEquals("ceiling fan", prefs.getString("chateauCeiling", ""))
        assertEquals("angry cow", prefs.getString("chateauMonsterName", ""))
    }

    @Test
    fun campaway_restIncrementsTimesRested() {
        val prefs = Preferences(MapSettings())
        PlaceSync.parseResponse(
            url = "place.php?whichplace=campaway&action=campaway_tentclick",
            html = "You take a free rest.",
            preferences = prefs,
        )
        assertEquals(true, prefs.getBoolean("getawayCampsiteUnlocked", false))
        assertEquals(1, prefs.getInt("timesRested", 0))
        assertEquals(true, prefs.getBoolean("_campAwayTentRested", false))
    }

    @Test
    fun falloutShelter_vault3SetsSpaPref() {
        val prefs = Preferences(MapSettings())
        PlaceSync.parseResponse(
            url = "place.php?whichplace=falloutshelter&action=vault3",
            html = "spa simulation",
            preferences = prefs,
        )
        assertEquals(true, prefs.getBoolean("falloutShelterAvailable", false))
        assertEquals(true, prefs.getBoolean(FalloutShelterRequest.SPA_USED_PREF, false))
    }

    @Test
    fun scrapheap_parsesEnergy() {
        val prefs = Preferences(MapSettings())
        PlaceSync.parseResponse(
            url = "place.php?whichplace=scrapheap",
            html = "You have 1,234 units of energy.",
            preferences = prefs,
        )
        assertEquals(1234, prefs.getInt("scrapheapEnergy", 0))
        assertEquals(true, prefs.getBoolean("scrapheapAvailable", false))
    }

    @Test
    fun getAdventuresUsed_campawayChateauFallout() {
        assertEquals(
            1,
            PlaceSync.getAdventuresUsed("place.php?whichplace=campaway&action=campaway_tentclick", 0),
        )
        assertEquals(
            0,
            PlaceSync.getAdventuresUsed("place.php?whichplace=campaway&action=campaway_tentclick", 1),
        )
        assertEquals(
            1,
            PlaceSync.getAdventuresUsed("place.php?whichplace=chateau&action=chateau_rest"),
        )
        assertEquals(
            1,
            PlaceSync.getAdventuresUsed("place.php?whichplace=falloutshelter&action=vault1"),
        )
        assertEquals(
            0,
            PlaceSync.getAdventuresUsed("place.php?whichplace=falloutshelter&action=vault3"),
        )
    }

    @Test
    fun arcade_parsesTickets() {
        val prefs = Preferences(MapSettings())
        PlaceSync.parseResponse(
            url = "place.php?whichplace=arcade",
            html = "You have 42 tickets.",
            preferences = prefs,
        )
        assertTrue(prefs.getBoolean("arcadeVisited", false))
        assertEquals(42, prefs.getInt("arcadeGameTickets", 0))
    }
}
