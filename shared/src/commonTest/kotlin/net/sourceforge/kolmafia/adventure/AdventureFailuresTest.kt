package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class AdventureFailuresTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        AdventureSession.resetForTest()
    }

    @Test
    fun table_has121EntriesWithBlankPageAtZero() {
        assertEquals(121, AdventureFailures.FAILURES.size)
        assertEquals("", AdventureFailures.FAILURES[0].responseText)
        assertEquals("KoL returned a blank page.", AdventureFailures.FAILURES[0].message)
    }

    @Test
    fun find_emptyResponse_returnsBlankPageIndex() {
        assertEquals(0, AdventureFailures.findAdventureFailure(""))
    }

    @Test
    fun find_outOfAdventures_isPending() {
        val index = AdventureFailures.findAdventureFailure("You're out of adventures today.")
        assertTrue(index > 0)
        assertEquals(AdventureFailureSeverity.PENDING, AdventureFailures.adventureFailureSeverity(index))
        assertEquals("You're out of adventures.", AdventureFailures.adventureFailureMessage(index))
        assertEquals(StopReason.NoAdventuresLeft, AdventureFailures.toStopReason(index))
    }

    @Test
    fun find_lockedArea_isError() {
        val index = AdventureFailures.findAdventureFailure("Seriously.  It's locked.")
        assertTrue(index > 0)
        assertEquals(AdventureFailureSeverity.ERROR, AdventureFailures.adventureFailureSeverity(index))
        val stop = AdventureFailures.toStopReason(index)
        assertTrue(stop is StopReason.AdventureFailure)
        assertFalse((stop as StopReason.AdventureFailure).pending)
    }

    @Test
    fun sideEffects_hippiesAndFratsAndDrippy() {
        AdventureFailures.findAdventureFailure("There are no Hippy soldiers left", prefs)
        assertEquals(1000, prefs.getInt("hippiesDefeated"))
        AdventureFailures.findAdventureFailure("There are no Frat soldiers left", prefs)
        assertEquals(1000, prefs.getInt("fratboysDefeated"))
        AdventureFailures.findAdventureFailure("Your Drippy Juice supply is empty", prefs)
        assertEquals(0, prefs.getInt("drippyJuice"))
    }

    @Test
    fun sideEffects_crimbo21ColdRes() {
        val html = "Better bundle up <b>[17 Cold Resistance Required]</b>"
        AdventureFailures.findAdventureFailure(html, prefs)
        assertEquals(17, prefs.getInt("_crimbo21ColdResistance"))
    }

    @Test
    fun session_skipsLogOnFailure() {
        AdventureSession.recordToSession("adventure.php?snarfblat=15", prefs)
        AdventureSession.setLastAdventure("The Spooky Forest", prefs, "adventure.php?snarfblat=15")
        val logged = AdventureSession.recordToSession(
            "adventure.php?snarfblat=15",
            "You're out of adventures",
            prefs,
        )
        assertFalse(logged)
    }

    @Test
    fun session_setLastAndClear() {
        AdventureSession.setLastAdventure("The Spooky Forest", prefs)
        assertNotNull(AdventureSession.lastVisitedLocationName)
        AdventureSession.clearLocation(prefs)
        assertEquals(null, AdventureSession.lastVisitedLocationName)
        assertEquals("None", prefs.getString("lastAdventure"))
    }

    @Test
    fun prettyName_dailyDungeonChamber() {
        val pretty = AdventureSession.getPrettyAdventureName(
            "The Daily Dungeon",
            "adventure.php?snarfblat=200&whichroom=3",
        )
        assertEquals("The Daily Dungeon (Chamber 3)", pretty)
    }
}
