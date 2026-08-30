package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.CakeArenaManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CakeArenaRequestTest {

    @BeforeTest
    fun reset() {
        CakeArenaManager.reset()
    }

    @Test
    fun parseVisit_registersOpponentsAndWins() {
        val prefs = Preferences(MapSettings())
        val html = """
            You have won 42 times. Only 8 wins left until your next prize!
            <tr><td valign=center><input type=radio name=whichopp value=1><b>Pork Soda</b> the Baby Gravy Fairy<br/>15 lbs.</tr>
            <tr><td valign=center><input type=radio name=whichopp value=2><b>Citrus Maximus</b> the Hovering Sombrero<br/>20 lbs.</tr>
        """.trimIndent()
        CakeArenaRequest.parseResponse("arena.php", html, preferences = prefs)
        assertEquals(2, CakeArenaManager.getOpponentList().size)
        assertEquals("Baby Gravy Fairy (15 lbs)", CakeArenaManager.getOpponent(1)?.toString())
        assertEquals(42, prefs.getInt("cakeArenaWins", 0))
        assertTrue(prefs.getBoolean("cakeArenaVisited", false))
    }

    @Test
    fun eventNameHelpers() {
        assertEquals("Scavenger Hunt", CakeArenaManager.eventIdToName(2))
        assertEquals(3, CakeArenaManager.eventNameToId("Obstacle Course"))
        assertEquals(0, CakeArenaManager.eventNameToId("Nope"))
    }

    @Test
    fun earnedXpAndAdventuresUsed() {
        val win = "Gorg is the winner, and gains 5 experience!"
        assertEquals(5, CakeArenaRequest.earnedXP(win))
        assertEquals(0, CakeArenaRequest.earnedXP("Gorg lost."))
        assertEquals(1, CakeArenaRequest.getAdventuresUsed("arena.php?action=go"))
        assertEquals(0, CakeArenaRequest.getAdventuresUsed("arena.php"))
    }

    @Test
    fun registerRequest_fightLogsContest() {
        CakeArenaManager.registerOpponent(1, "Pork Soda", "Baby Gravy Fairy", 15)
        assertTrue(
            CakeArenaRequest.registerRequest("arena.php?action=go&whichopp=1&event=2"),
        )
        assertTrue(CakeArenaRequest.registerRequest("arena.php"))
        assertFalse(CakeArenaRequest.registerRequest("bounty.php"))
    }

    @Test
    fun fightResponse_detectsSuckage() {
        val html = """
            <table><tr><td>You enter Ton against Mr. Joe Bangles in an Obstacle Course race.<p>
            Ton is too short to get over most of the obstacles.<p>
            Ton makes it through the obstacle course in 199 seconds.<p>
            Ton is the winner, and gains 5 experience!</td></tr></table>
        """.trimIndent()
        val lines = CakeArenaRequest.contestLines(html)
        assertNotNull(lines)
        assertTrue(lines!!.size >= 2)
        assertTrue(lines[1].contains("too short"))
    }
}
