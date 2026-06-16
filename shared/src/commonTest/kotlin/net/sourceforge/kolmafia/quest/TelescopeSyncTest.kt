package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class TelescopeSyncTest {

    @Test
    fun parseResponse_lowTelescopeSyncsCrowdAndTrapChallenges() {
        val prefs = Preferences(MapSettings())
        val html = """
            You adjust the focus and see a second group of people standing around flexing their muscles and using grip exercisers.
            You scan to the right a bit and see a third group of people, all of whom appear to be on fire.
            You sweep the telescope up to reveal some smoldering bushes on the outskirts of a hedge maze.
            Beyond the maze's entrance you see smoke rising from deeper within the maze.
            You focus the telescope on the back side of the keep, somehow, and see a pipe with lava slowly oozing out of it.
        """.trimIndent()
        TelescopeSync.parseResponse(
            "https://www.kingdomofloathing.com/campground.php?action=telescopelow",
            html,
            prefs,
        )
        assertEquals(
            "standing around flexing their muscles and using grip exercisers",
            prefs.getString("telescope1", ""),
        )
        assertEquals("Muscle", prefs.getString("nsChallenge1", ""))
        assertEquals("hot", prefs.getString("nsChallenge2", ""))
        assertEquals("hot", prefs.getString("nsChallenge3", ""))
        assertEquals("hot", prefs.getString("nsChallenge4", ""))
        assertEquals("hot", prefs.getString("nsChallenge5", ""))
        assertEquals(5, prefs.getInt("telescopeUpgrades", 0))
    }

    @Test
    fun parseResponse_highTelescopeSetsLookedHigh() {
        val prefs = Preferences(MapSettings())
        TelescopeSync.parseResponse(
            "campground.php?action=telescopehigh",
            "<html>Starry</html>",
            prefs,
        )
        assertEquals(true, prefs.getBoolean("telescopeLookedHigh", false))
    }
}
