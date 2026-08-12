package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandWarResetSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun seedStaleWarPrefs(preferences: Preferences) {
        preferences.setInt("fratboysDefeated", 500)
        preferences.setInt("hippiesDefeated", 400)
        preferences.setString("sidequestArenaCompleted", "hippy")
        preferences.setString("sidequestFarmCompleted", "fratboy")
        preferences.setString("sidequestJunkyardCompleted", "hippy")
        preferences.setString("sidequestLighthouseCompleted", "fratboy")
        preferences.setString("sidequestNunsCompleted", "hippy")
        preferences.setString("sidequestOrchardCompleted", "fratboy")
        preferences.setString("currentJunkyardTool", "molybdenum hammer")
        preferences.setString("currentJunkyardLocation", "near an abandoned refrigerator")
        preferences.setInt("currentNunneryMeat", 10000)
        preferences.setInt("lastFratboyCall", 3)
        preferences.setInt("lastHippyCall", 2)
        preferences.setInt("availableDimes", 50)
        preferences.setInt("availableQuarters", 25)
        preferences.setString("sideDefeated", "hippies")
        preferences.setString("warProgress", "started")
        preferences.setInt("flyeredML", 999)
    }

    @Test
    fun ensureUpdated_skipsWhenAlreadyCurrent() {
        val prefs = prefs()
        prefs.setInt(IslandWarResetSync.PREF_LAST_BATTLEFIELD_RESET, 5)
        seedStaleWarPrefs(prefs)

        assertFalse(IslandWarResetSync.ensureUpdated(5, prefs))

        assertEquals(5, prefs.getInt(IslandWarResetSync.PREF_LAST_BATTLEFIELD_RESET, -1))
        assertEquals(500, prefs.getInt("fratboysDefeated", 0))
        assertEquals("started", prefs.getString("warProgress", ""))
    }

    @Test
    fun ensureUpdated_resetsWhenAscensionIncreased() {
        val prefs = prefs()
        prefs.setInt(IslandWarResetSync.PREF_LAST_BATTLEFIELD_RESET, 4)
        seedStaleWarPrefs(prefs)
        prefs.setString("currentHippyStore", "none")

        assertTrue(IslandWarResetSync.ensureUpdated(5, prefs))

        assertEquals(5, prefs.getInt(IslandWarResetSync.PREF_LAST_BATTLEFIELD_RESET, -1))
        assertEquals(0, prefs.getInt("fratboysDefeated", -1))
        assertEquals(0, prefs.getInt("hippiesDefeated", -1))
        assertEquals("none", prefs.getString("sidequestArenaCompleted", ""))
        assertEquals("none", prefs.getString("sidequestFarmCompleted", ""))
        assertEquals("none", prefs.getString("sidequestJunkyardCompleted", ""))
        assertEquals("none", prefs.getString("sidequestLighthouseCompleted", ""))
        assertEquals("none", prefs.getString("sidequestNunsCompleted", ""))
        assertEquals("none", prefs.getString("sidequestOrchardCompleted", ""))
        assertEquals("", prefs.getString("currentJunkyardTool", "x"))
        assertEquals("", prefs.getString("currentJunkyardLocation", "x"))
        assertEquals(0, prefs.getInt("currentNunneryMeat", -1))
        assertEquals(-1, prefs.getInt("lastFratboyCall", 0))
        assertEquals(-1, prefs.getInt("lastHippyCall", 0))
        assertEquals(0, prefs.getInt("availableDimes", -1))
        assertEquals(0, prefs.getInt("availableQuarters", -1))
        assertEquals("neither", prefs.getString("sideDefeated", ""))
        assertEquals("unstarted", prefs.getString("warProgress", ""))
        assertEquals(0, prefs.getInt("flyeredML", -1))
        assertEquals("none", prefs.getString("currentHippyStore", ""))
    }

    @Test
    fun resetIsland_orchardCompletedCopiesCurrentHippyStore() {
        val prefs = prefs()
        prefs.setString("currentHippyStore", "hippy")
        prefs.setString("sidequestOrchardCompleted", "fratboy")

        IslandWarResetSync.resetIsland(prefs)

        assertEquals("hippy", prefs.getString("sidequestOrchardCompleted", ""))
        assertEquals("hippy", prefs.getString("currentHippyStore", ""))
    }

    @Test
    fun ensureUpdated_firstLogin() {
        val prefs = prefs()
        seedStaleWarPrefs(prefs)

        assertTrue(IslandWarResetSync.ensureUpdated(0, prefs))

        assertEquals(0, prefs.getInt(IslandWarResetSync.PREF_LAST_BATTLEFIELD_RESET, -1))
        assertEquals("unstarted", prefs.getString("warProgress", ""))
        assertEquals(0, prefs.getInt("fratboysDefeated", -1))
    }
}
