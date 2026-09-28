package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UseItemEvHelmetSyncTest {
    private val url = "inv_use.php?whichitem=3162&dowhichitem=3146"

    private fun prefs() = Preferences(MapSettings())

    private fun page(header: String, levels: List<String>): String {
        val html = StringBuilder("<td>")
        html.append(header)
        html.append("</td>")
        levels.forEach { level ->
            html.append("<td>").append(level).append("</td><td>next:</td>")
        }
        html.append("<td>tail</td><td>end</td><td>more</td>")
        return "A voice speaks (for a long time) from the helmet. $html"
    }

    @Test
    fun punchcard_storesConduitLevelsAndAscension() {
        val prefs = prefs()
        val levels = List(9) { "ZERO" }.toMutableList()
        levels[0] = "FU"
        val ok = UseItemEvHelmetSync.parse(url, page("KROKRO LAZAK FULA:", levels), prefs, ascensions = 6)
        assertTrue(ok)
        assertEquals(7 * pow11(8), prefs.getInt("lastEVHelmetValue", -1))
        assertEquals(6, prefs.getInt("lastEVHelmetReset", -1))
    }

    @Test
    fun spearHeader_storesLevels() {
        val prefs = prefs()
        val ok = UseItemEvHelmetSync.parse(
            url,
            page("SPEAR POWER CONDUIT:", List(9) { "ZERO" }),
            prefs,
            ascensions = 2,
        )
        assertTrue(ok)
        assertEquals(0, prefs.getInt("lastEVHelmetValue", -1))
        assertEquals(2, prefs.getInt("lastEVHelmetReset", -1))
    }

    @Test
    fun droneVoice_doesNotStore() {
        val prefs = prefs()
        prefs.setInt("lastEVHelmetValue", 4)
        val ok = UseItemEvHelmetSync.parse(
            url,
            "A tinny voice emerges from the drone. " + page("KROKRO LAZAK FULA:", List(9) { "ZERO" }),
            prefs,
            ascensions = 1,
        )
        assertFalse(ok)
        assertEquals(4, prefs.getInt("lastEVHelmetValue", -1))
    }

    @Test
    fun missingSpeech_orUnknownLevel_leavesPrefs() {
        val prefs = prefs()
        prefs.setInt("lastEVHelmetValue", 9)
        assertFalse(
            UseItemEvHelmetSync.parse(
                url,
                page("KROKRO LAZAK FULA:", List(9) { "ZERO" }).replace("(for a long time)", "briefly"),
                prefs,
                1,
            ),
        )
        assertFalse(
            UseItemEvHelmetSync.parse(
                url,
                page("KROKRO LAZAK FULA:", List(9) { "BOGUS" }),
                prefs,
                1,
            ),
        )
        assertFalse(UseItemEvHelmetSync.parse("inv_use.php?whichitem=3162&dowhichitem=1", page("KROKRO LAZAK FULA:", List(9) { "ZERO" }), prefs, 1))
        assertEquals(9, prefs.getInt("lastEVHelmetValue", -1))
    }

    private fun pow11(exp: Int): Int {
        var value = 1
        repeat(exp) { value *= 11 }
        return value
    }
}
