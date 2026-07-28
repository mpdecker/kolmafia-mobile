package net.sourceforge.kolmafia.campground

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

class MushroomPlotSyncTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun hasPlot_detectsPlotHeader() {
        assertTrue(MushroomPlotSync.hasPlot("<b>Your Mushroom Plot:</b><p><table></table>"))
        assertFalse(MushroomPlotSync.hasPlot("<html>buy a plot first</html>"))
    }

    @Test
    fun apply_setsLastMushroomPlotPrefWhenPlotPresent() {
        val p = prefs()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(ascensions = "7"))
        }
        MushroomPlotSync.apply(
            p,
            char,
            """<b>Your Mushroom Plot:</b><p><table><tr><td></td></tr></table>""",
            "https://www.kingdomofloathing.com/knoll_mushrooms.php",
        )
        assertEquals(7, p.getInt("lastMushroomPlot", -1))
    }

    @Test
    fun apply_skipsWhenPlotAbsent() {
        val p = prefs()
        val char = KoLCharacter()
        MushroomPlotSync.apply(
            p,
            char,
            """<html>You haven't bought a mushroom plot yet.</html>""",
            "https://www.kingdomofloathing.com/knoll_mushrooms.php",
        )
        assertEquals(-1, p.getInt("lastMushroomPlot", -1))
    }

    @Test
    fun apply_skipsNonKnollUrl() {
        val p = prefs()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(ascensions = "3"))
        }
        MushroomPlotSync.apply(
            p,
            char,
            """<b>Your Mushroom Plot:</b><p><table></table>""",
            "https://www.kingdomofloathing.com/campground.php",
        )
        assertEquals(-1, p.getInt("lastMushroomPlot", -1))
    }
}
