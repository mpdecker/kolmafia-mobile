package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AfterLifeRequestTest {

    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        CharpaneValhallaSync.reset()
    }

    @AfterTest
    fun tearDown() {
        CharpaneValhallaSync.reset()
    }

    @Test
    fun pearlyGates_parsesKarmaGain() {
        prefs.setInt("bankedKarma", 100)
        val html = "<td valign=center>You gain 311 Karma</td>"
        val refreshed = AfterLifeRequest.parseResponse(
            "afterlife.php?action=pearlygates",
            html,
            prefs,
        )
        assertTrue(refreshed)
        assertEquals(411, prefs.getInt("bankedKarma", 0))
        assertTrue(CharpaneValhallaSync.inValhalla)
    }

    @Test
    fun buyDeli_spendsKarma() {
        prefs.setInt("bankedKarma", 5)
        AfterLifeRequest.parseResponse(
            "afterlife.php?action=buydeli&whichitem=5045",
            "You spend 1 Karma",
            prefs,
        )
        assertEquals(4, prefs.getInt("bankedKarma", 0))
    }

    @Test
    fun emptyResponse_returnsFalse() {
        prefs.setInt("lastBreakfast", 0)
        assertFalse(AfterLifeRequest.parseResponse("afterlife.php", "", prefs))
        assertEquals(0, prefs.getInt("lastBreakfast", -1))
        assertFalse(CharpaneValhallaSync.inValhalla)
    }

    @Test
    fun firstAfterlifeVisit_runsOnAscensionWhenLastBreakfastSet() {
        prefs.setInt("lastBreakfast", 0)
        prefs.setInt("knownAscensions", 4)
        assertTrue(AfterLifeRequest.parseResponse("afterlife.php", "<html>Valhalla</html>", prefs))
        assertEquals(-1, prefs.getInt("lastBreakfast", 0))
        assertEquals(5, prefs.getInt("knownAscensions", 0))
        assertTrue(CharpaneValhallaSync.inValhalla)
    }

    @Test
    fun registerRequest_ascendConfirm_includesSignPathAndKarma() {
        prefs.setInt("bankedKarma", 77)
        val url =
            "afterlife.php?action=ascend&confirmascend=1&asctype=3&gender=2&whichclass=4&whichpath=4&whichsign=2"
        val logger = net.sourceforge.kolmafia.session.SessionLogger(
            prefs,
            net.sourceforge.kolmafia.event.GameEventBus(),
        )
        AfterLifeRequest.registerRequest(url, logger, prefs)
        val lines = logger.recentLines()
        assertTrue(
            lines.any {
                it.contains("Hardcore") &&
                    it.contains("Female") &&
                    it.contains("Sauceror") &&
                    it.contains("Wallaby") &&
                    it.contains("Bees Hate You") &&
                    it.contains("77")
            },
            lines.joinToString("\n"),
        )
    }

    @Test
    fun reincarnateClassName_desktopKoLIds() {
        assertEquals("Ed the Undying", AfterLifeRequest.reincarnateClassName(17))
        assertEquals("Cow Puncher", AfterLifeRequest.reincarnateClassName(18))
        assertEquals("Grey Goo", AfterLifeRequest.reincarnateClassName(27))
    }
}
