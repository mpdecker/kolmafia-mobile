package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BreakfastManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IslandWarPathsTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun currentIsland_unstarted_returnsBogus() {
        val p = prefs { putString("warProgress", "unstarted") }
        assertEquals("bogus.php", IslandWarPaths.currentIsland(p))
    }

    @Test
    fun currentIsland_started_returnsBigIsland() {
        val p = prefs { putString("warProgress", "started") }
        assertEquals("bigisland.php", IslandWarPaths.currentIsland(p))
    }

    @Test
    fun currentIsland_finished_returnsPostwar() {
        val p = prefs { putString("warProgress", "finished") }
        assertEquals("postwarisland.php", IslandWarPaths.currentIsland(p))
    }

    @Test
    fun questCompleter_hippy_returnsHippies() {
        val p = prefs { putString("sidequestArenaCompleted", "hippy") }
        assertEquals("hippies", IslandWarPaths.questCompleter("sidequestArenaCompleted", p))
    }

    @Test
    fun questCompleter_fratboy_returnsFratboys() {
        val p = prefs { putString("sidequestNunsCompleted", "fratboy") }
        assertEquals("fratboys", IslandWarPaths.questCompleter("sidequestNunsCompleted", p))
    }

    @Test
    fun questCompleter_none_returnsNone() {
        val p = prefs { putString("sidequestArenaCompleted", "none") }
        assertEquals("none", IslandWarPaths.questCompleter("sidequestArenaCompleted", p))
    }

    @Test
    fun sidequestOutfit_returnsHippyWhenAvailable() {
        val p = prefs { putString("sidequestFarmCompleted", "hippy") }
        assertEquals(
            BreakfastManager.WarSideOutfit.HIPPY,
            BreakfastManager.sidequestOutfit(
                "sidequestFarmCompleted",
                p,
                hippyAvailable = true,
                fratboyAvailable = false,
            ),
        )
    }

    @Test
    fun sidequestOutfit_returnsNullWhenSideUnavailable() {
        val p = prefs { putString("sidequestLighthouseCompleted", "fratboy") }
        assertNull(
            BreakfastManager.sidequestOutfit(
                "sidequestLighthouseCompleted",
                p,
                hippyAvailable = true,
                fratboyAvailable = false,
            ),
        )
    }

    @Test
    fun sidequestOutfit_none_returnsNull() {
        val p = prefs { putString("sidequestFarmCompleted", "none") }
        assertNull(
            BreakfastManager.sidequestOutfit(
                "sidequestFarmCompleted",
                p,
                hippyAvailable = true,
                fratboyAvailable = true,
            ),
        )
    }
}
