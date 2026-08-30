package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
