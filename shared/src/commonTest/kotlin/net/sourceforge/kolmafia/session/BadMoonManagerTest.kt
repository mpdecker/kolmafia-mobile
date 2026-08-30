package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BadMoonManagerTest {

    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
    }

    @Test
    fun registerAdventure_setsPrefKey() {
        BadMoonManager.registerAdventure("O Goblin, Where Art Thou?", prefs, ascensionNumber = 1)
        assertTrue(prefs.getBoolean("badMoonEncounter01", false))
    }

    @Test
    fun validateBadMoon_resetsOnNewAscension() {
        prefs.setBoolean("badMoonEncounter01", true)
        prefs.setInt("lastBadMoonReset", 1)
        BadMoonManager.validateBadMoon(prefs, ascensionNumber = 2)
        assertFalse(prefs.getBoolean("badMoonEncounter01", false))
        assertEquals(2, prefs.getInt("lastBadMoonReset", 0))
    }

    @Test
    fun completedCount_tracksPrefs() {
        prefs.setBoolean("badMoonEncounter01", true)
        prefs.setBoolean("badMoonEncounter02", true)
        assertEquals(2, BadMoonManager.completedCount(prefs))
    }
}
