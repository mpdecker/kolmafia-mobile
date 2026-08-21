package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BeachCombManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP845Test {
    @Test
    fun beachMapTracksMinutesLayoutTidesAndTwinkles() {
        val prefs = Preferences(MapSettings())
        val html = """
            You walk for 4,242 minutes and find a nice stretch of beach.
            <input name="coords" value="10,42420"><span title="rough sand with a twinkle"><img src="otherimages/beachcomb/twinkle.gif">
            <input name="coords" value="10,42419"><span title="rough sand"><img src="otherimages/beachcomb/rough.gif">
            <input name="coords" value="9,42420"><span title="combed sand"><img src="otherimages/beachcomb/combed.gif">
        """.trimIndent()
        assertTrue(BeachCombManager.parseBeachMap(html, prefs))
        assertEquals(4242, prefs.getInt("_beachMinutes", 0))
        assertEquals("9:c,10:tr", prefs.getString("_beachLayout", ""))
        assertEquals(8, prefs.getInt("_beachTides", 0))
        assertTrue(prefs.getBoolean("hasTwinkleVision", false))
    }
}
