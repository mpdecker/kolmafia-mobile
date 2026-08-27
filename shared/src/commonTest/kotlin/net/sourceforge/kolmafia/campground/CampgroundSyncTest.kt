package net.sourceforge.kolmafia.campground

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

class CampgroundSyncTest {

    @Test
    fun parseCampground_setsDwellingAndKitchenAndBookshelf() {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter()
        val html = """
            <img src="/images/itemimages/rest3.gif">
            <img src="ezcook.gif">
            <img src="wbchemset.gif">
            <a href="campground.php?action=bookshelf">Bookshelf</a>
        """.trimIndent()
        CampgroundSync.parseResponse(
            url = "campground.php",
            html = html,
            preferences = prefs,
            character = char,
        )
        assertEquals(143, prefs.getInt(DwellingSync.CURRENT_DWELLING_ITEM_ID_PREF, -1))
        assertEquals(true, prefs.getBoolean("hasOven", false))
        assertEquals(6967, prefs.getInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, -1))
        assertEquals(true, char.state.value.hasBookshelf)
    }

    @Test
    fun parseFurnishings_storesFurnitureNames() {
        val prefs = Preferences(MapSettings())
        CampgroundSync.parseResponse(
            url = "campground.php?action=inspectdwelling",
            html = "<b>Furnishings</b><b>a padded cot</b><b>a lamp</b>",
            preferences = prefs,
        )
        val furnishings = prefs.getString("_campgroundFurnishings", "")
        assertTrue(furnishings.contains("padded cot"), furnishings)
        assertTrue(furnishings.contains("lamp"), furnishings)
    }

    @Test
    fun parseRest_incrementsTimesRestedAndFreeRests() {
        val prefs = Preferences(MapSettings())
        CampgroundSync.parseResponse(
            url = "campground.php?action=rest",
            html = "You take a free rest in your tent.",
            preferences = prefs,
        )
        assertEquals(1, prefs.getInt("timesRested", 0))
        assertEquals(1, prefs.getInt("_freeRestsUsed", 0))
    }

    @Test
    fun getAdventuresUsed_restRespectsFreeRests() {
        assertEquals(0, CampgroundSync.getAdventuresUsed("campground.php?action=rest", 2))
        assertEquals(1, CampgroundSync.getAdventuresUsed("campground.php?action=rest", 0))
        assertEquals(0, CampgroundSync.getAdventuresUsed("campground.php?action=garden", 0))
    }

    @Test
    fun parseDna_setsSyringePref() {
        val prefs = Preferences(MapSettings())
        CampgroundSync.parseDnaAndCmc(
            "You have a sample of <b>human</b> DNA.",
            prefs,
        )
        assertEquals("human", prefs.getString("dnaSyringe", ""))
    }

    @Test
    fun gardenHarvest_setsHarvestedFlag() {
        val prefs = Preferences(MapSettings())
        CampgroundSync.parseResponse(
            url = "campground.php?action=garden",
            html = """<img src="pumpkinpatch_2.gif">""",
            preferences = prefs,
        )
        assertEquals(true, prefs.getBoolean("_gardenHarvested", false))
    }
}
