package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParseResultsResidualTest {

    @Test
    fun lucky_setsFallbackPrefWithoutEffectManager() {
        val prefs = Preferences(MapSettings())
        val out = ParseResultsResidual.apply(
            urlString = "adventure.php?snarfblat=1",
            responseText = "You feel less lucky",
            preferences = prefs,
        )
        assertTrue(out.luckyRemoved)
        assertTrue(prefs.getBoolean("_luckyRemovedByParseResults", false))
    }

    @Test
    fun godLobsterMaxed_setsThree() {
        val prefs = Preferences(MapSettings())
        val out = ParseResultsResidual.apply(
            urlString = "main.php?fightgodlobster=1",
            responseText = "You can't challenge your God Lobster anymore today.",
            preferences = prefs,
        )
        assertTrue(out.godLobsterMaxed)
        assertEquals(3, prefs.getInt("_godLobsterFights", 0))
    }

    @Test
    fun ascendFail_fromMain() {
        val out = ParseResultsResidual.apply(
            urlString = "main.php?nope=asc",
            responseText = "You may not enter the Astral Gash again until tomorrow.",
            preferences = Preferences(MapSettings()),
        )
        assertNotNull(out.ascendFailMessage)
        assertTrue(out.ascendFailMessage!!.contains("Astral Gash"))
    }

    @Test
    fun butlerMeat_logged() {
        val out = ParseResultsResidual.apply(
            urlString = "campground.php",
            responseText = ParseResultsResidual.BUTLER_MEAT,
            preferences = Preferences(MapSettings()),
        )
        assertTrue(out.butlerLogged)
    }
}
