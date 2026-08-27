package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BeachCombChoiceSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP847Test {
    @Test
    fun combingCoordinateMarksLayoutSquare() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_beachLayout", "8:rrrrrrrrrr")
        assertTrue(
            BeachCombChoiceSync.apply(
                choiceId = 1388,
                decision = 4,
                html = "You comb the area and find something kind of interesting.",
                preferences = prefs,
                choiceUrl = "whichchoice=1388&option=4&coords=8%2C4197",
            ),
        )
        assertEquals("8:rrrcrrrrrr", prefs.getString("_beachLayout", ""))
    }
}
