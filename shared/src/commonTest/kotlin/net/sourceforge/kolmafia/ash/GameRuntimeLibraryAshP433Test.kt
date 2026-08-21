package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.campground.GardenCropIds
import net.sourceforge.kolmafia.campground.MushroomPlotSync
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP433Test {

    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    private fun campgroundDb(): GameDatabase = object : GameDatabase() {
        private val items = mapOf(
            4761 to ItemData(4761, "pumpkin", "desc", "pumpkin.gif", ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null),
            4760 to ItemData(4760, "pumpkin seed", "desc", "pumpkinseed.gif", ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null),
        )
        override fun item(id: Int): ItemData? = items[id]
        override fun item(name: String): ItemData? =
            items.values.find { it.name.equals(name, ignoreCase = true) }
    }

    @Test
    fun revision_phase480() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun campgroundVisitHook_populatesCropInGetCampground() {
        val p = prefs()
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        val lib = GameRuntimeLibrary(character = char, preferences = p, gameDatabase = campgroundDb())
        lib.processVisitResponseHooks(
            """<img src="pumpkinpatch_2.gif">""",
            "https://www.kingdomofloathing.com/campground.php",
        )
        assertEquals(2, CampgroundInventorySync.load(p)[GardenCropIds.PUMPKIN])
        assertTrue(outputLib(lib, """print(count(get_campground()));""").trim().toInt() >= 1)
    }

    @Test
    fun knollVisitHook_writesMushroomPlotSquaresPref() {
        val p = prefs()
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        val lib = GameRuntimeLibrary(character = char, preferences = p)
        lib.processVisitResponseHooks(
            """
            <b>Your Mushroom Plot:</b><p><table>
            <tr><td><img src="mushroom.gif"></td><td><img src="dirt1.gif"></td><td><img src="mushsprout.gif"></td><td><img src="spooshroom.gif"></td></tr>
            <tr><td><img src="dirt1.gif"></td><td><img src="dirt1.gif"></td><td><img src="dirt1.gif"></td><td><img src="dirt1.gif"></td></tr>
            <tr><td><img src="dirt1.gif"></td><td><img src="dirt1.gif"></td><td><img src="dirt1.gif"></td><td><img src="dirt1.gif"></td></tr>
            <tr><td><img src="dirt1.gif"></td><td><img src="dirt1.gif"></td><td><img src="dirt1.gif"></td><td><img src="dirt1.gif"></td></tr>
            </table>
            """.trimIndent(),
            "https://www.kingdomofloathing.com/knoll_mushrooms.php",
        )
        assertEquals("kb__..sp________________________", p.getString(MushroomPlotSync.MUSHROOM_PLOT_SQUARES_PREF, ""))
    }
}
