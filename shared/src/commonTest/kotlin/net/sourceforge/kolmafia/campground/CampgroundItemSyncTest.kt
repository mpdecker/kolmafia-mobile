package net.sourceforge.kolmafia.campground

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences

class CampgroundItemSyncTest {

    @Test
    fun syncFromHtml_setsWorkshedItemFromGif() {
        val prefs = Preferences(MapSettings())
        CampgroundItemSync.syncFromHtml(
            """<img src="wbchemset.gif">""",
            prefs,
        )
        assertEquals(6967, prefs.getInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, -1))
    }

    @Test
    fun syncFromHtml_setsColdMedicineCabinetFromDoctorsOutText() {
        val prefs = Preferences(MapSettings())
        CampgroundItemSync.syncFromHtml(
            "Looks like the doctors are out for the day.",
            prefs,
        )
        assertEquals(10815, prefs.getInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, -1))
    }

    @Test
    fun apply_onlyRunsForCampgroundUrl() {
        val prefs = Preferences(MapSettings())
        CampgroundItemSync.apply(prefs, """<img src="wbchemset.gif">""", "shop.php")
        assertEquals(-1, prefs.getInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, -1))
        CampgroundItemSync.apply(
            prefs,
            """<img src="wbchemset.gif">""",
            "https://www.kingdomofloathing.com/campground.php",
        )
        assertEquals(6967, prefs.getInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, -1))
    }

    @Test
    fun syncFromHtml_setsBurningLeavesPref() {
        val prefs = Preferences(MapSettings())
        CampgroundItemSync.syncFromHtml(
            """<img src="burningleaves.gif">""",
            prefs,
        )
        assertEquals(true, prefs.getBoolean(CampgroundItemSync.CAMPGROUND_HAS_BURNING_LEAVES_PREF, false))
    }

    @Test
    fun syncFromHtml_setsSourceTerminalPref() {
        val prefs = Preferences(MapSettings())
        CampgroundItemSync.syncFromHtml(
            """<img src="sourceterminal.gif">""",
            prefs,
        )
        assertEquals(true, prefs.getBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, false))
    }

    @Test
    fun syncFromHtml_setsKitchenEquipmentPrefs() {
        val prefs = Preferences(MapSettings())
        CampgroundItemSync.syncFromHtml(
            """
            <img src="ezcook.gif">
            <img src="oven.gif">
            <img src="shaker.gif">
            <img src="cocktailkit.gif">
            <img src="chefinbox.gif">
            <img src="bartinbox.gif">
            """.trimIndent(),
            prefs,
        )
        assertEquals(true, prefs.getBoolean("hasOven", false))
        assertEquals(true, prefs.getBoolean("hasRange", false))
        assertEquals(true, prefs.getBoolean("hasShaker", false))
        assertEquals(true, prefs.getBoolean("hasCocktailKit", false))
        assertEquals(true, prefs.getBoolean("hasChef", false))
        assertEquals(true, prefs.getBoolean("hasBartender", false))
    }

    @Test
    fun apply_setsHasBookshelfFromCampgroundHtml() {
        val prefs = Preferences(MapSettings())
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        CampgroundItemSync.apply(
            prefs,
            """<a href="campground.php?action=bookshelf">Bookshelf</a>""",
            "https://www.kingdomofloathing.com/campground.php",
            char,
        )
        assertEquals(true, char.state.value.hasBookshelf)
    }
}
