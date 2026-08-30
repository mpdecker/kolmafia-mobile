package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GrimstoneManagerTest {

    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        RumpleManager.resetForTest()
        prefs = Preferences(MapSettings())
    }

    @AfterTest
    fun tearDown() {
        RumpleManager.resetForTest()
    }

    @Test
    fun maskChoice_startsStepmotherPath() {
        assertTrue(
            GrimstoneManager.apply(
                choiceId = GrimstoneManager.MASK_CHOICE,
                html = "",
                preferences = prefs,
                decision = 1,
            ),
        )
        assertEquals("stepmother", prefs.getString("grimstoneMaskPath", ""))
        assertEquals(30, prefs.getInt("cinderellaMinutesToMidnight", 0))
        assertTrue(GrimstoneManager.zoneGateOpen(prefs))
    }

    @Test
    fun cinderellaVisit_and_score() {
        GrimstoneManager.applyVisit(822, "<i>It is 28 minutes to midnight.</i>", prefs)
        assertEquals("stepmother", prefs.getString("grimstoneMaskPath", ""))
        assertEquals(28, prefs.getInt("cinderellaMinutesToMidnight", 0))
        GrimstoneManager.apply(
            822,
            "Your score is now <b>42</b>",
            prefs,
            decision = 1,
        )
        assertEquals(42, prefs.getInt("cinderellaScore", 0))
    }

    @Test
    fun wolfAndWitchTurnCounters() {
        GrimstoneManager.apply(832, "", prefs, decision = 1)
        assertEquals(1, prefs.getInt("wolfTurnsUsed", 0))
        GrimstoneManager.apply(837, "", prefs, decision = 1)
        assertEquals(1, prefs.getInt("candyWitchTurnsUsed", 0))
        GrimstoneManager.incrementFights(369, prefs)
        assertEquals(2, prefs.getInt("wolfTurnsUsed", 0))
        GrimstoneManager.incrementFights(380, prefs)
        assertEquals(1, prefs.getInt("rumpelstiltskinTurnsUsed", 0))
    }

    @Test
    fun isGrimstoneAdventure() {
        assertTrue(GrimstoneManager.isGrimstoneAdventure(374))
        assertTrue(GrimstoneManager.isGrimstoneAdventure(null, "ioty2014_wolf"))
    }
}
