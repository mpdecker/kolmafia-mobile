package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IslandWarConcertTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun arenaOpen(side: String = "hippy"): Preferences = prefs {
        putString("warProgress", "started")
        putString("sidequestArenaCompleted", side)
        putString("sideDefeated", "neither")
    }

    @Test
    fun effectToConcertNumber_hippyPrefixMatch() {
        val p = arenaOpen("hippy")
        assertEquals(1, IslandWarConcert.effectToConcertNumber("hippies", "moon", p))
        assertEquals(2, IslandWarConcert.effectToConcertNumber("hippies", "dilated", p))
        assertEquals(3, IslandWarConcert.effectToConcertNumber("hippies", "optimist", p))
    }

    @Test
    fun effectToConcertNumber_fratboyPrefixMatch() {
        val p = arenaOpen("fratboy")
        assertEquals(1, IslandWarConcert.effectToConcertNumber("fratboys", "elvish", p))
        assertEquals(2, IslandWarConcert.effectToConcertNumber("fratboys", "wi", p))
        assertEquals(3, IslandWarConcert.effectToConcertNumber("fratboys", "wh", p))
    }

    @Test
    fun effectToConcertNumber_sideDefeatedBlocks() {
        val p = prefs {
            putString("warProgress", "finished")
            putString("sidequestArenaCompleted", "hippy")
            putString("sideDefeated", "hippies")
        }
        assertEquals(0, IslandWarConcert.effectToConcertNumber("hippies", "moon", p))
    }

    @Test
    fun effectToConcertNumber_bothDefeatedBlocks() {
        val p = prefs {
            putString("sideDefeated", "both")
        }
        assertEquals(0, IslandWarConcert.effectToConcertNumber("fratboys", "elvish", p))
    }

    @Test
    fun concertError_unstarted() {
        val p = prefs { putString("warProgress", "unstarted") }
        assertEquals(
            "You have not started the island war yet.",
            IslandWarConcert.concertError("moon", p),
        )
    }

    @Test
    fun concertError_arenaNotOpen() {
        val p = prefs {
            putString("warProgress", "started")
            putString("sidequestArenaCompleted", "none")
        }
        assertEquals("The arena is not open.", IslandWarConcert.concertError("moon", p))
    }

    @Test
    fun concertError_fansDefeated() {
        val p = prefs {
            putString("warProgress", "finished")
            putString("sidequestArenaCompleted", "fratboy")
            putString("sideDefeated", "fratboys")
        }
        assertEquals(
            "The arena's fans were defeated in the war.",
            IslandWarConcert.concertError("elvish", p),
        )
    }

    @Test
    fun concertError_invalidNumber() {
        val p = arenaOpen("hippy")
        assertEquals("Invalid concert number.", IslandWarConcert.concertError("9", p))
    }

    @Test
    fun concertError_wrongSideEffect() {
        val p = arenaOpen("hippy")
        assertEquals(
            "The \"elvish\" effect is not available to hippies.",
            IslandWarConcert.concertError("elvish", p),
        )
    }

    @Test
    fun concertError_validEffect_returnsEmpty() {
        val p = arenaOpen("hippy")
        assertEquals("", IslandWarConcert.concertError("moon", p))
    }

    @Test
    fun resolveConcertOption_digit() {
        val p = arenaOpen("hippy")
        assertEquals(2, IslandWarConcert.resolveConcertOption("2", p))
    }

    @Test
    fun resolveConcertOption_effectName() {
        val p = arenaOpen("fratboy")
        assertEquals(2, IslandWarConcert.resolveConcertOption("winklered", p))
    }

    @Test
    fun resolveConcertOption_bogusIsland_null() {
        val p = prefs {
            putString("warProgress", "unstarted")
            putString("sidequestArenaCompleted", "hippy")
        }
        assertNull(IslandWarConcert.resolveConcertOption("1", p))
    }

    @Test
    fun resolveConcertOption_invalidEffect_null() {
        val p = arenaOpen("hippy")
        assertNull(IslandWarConcert.resolveConcertOption("elvish", p))
    }

    @Test
    fun concertUrl_buildsQuery() {
        val p = arenaOpen("hippy")
        assertEquals(
            "bigisland.php?action=concert&option=1",
            IslandWarConcert.concertUrl(1, p),
        )
    }

    @Test
    fun nunneryUrl_buildsQuery() {
        val p = prefs { putString("warProgress", "finished") }
        assertEquals(
            "postwarisland.php?place=nunnery&action=nuns",
            IslandWarConcert.nunneryUrl(p),
        )
    }

    @Test
    fun nunneryUrl_bogus_null() {
        val p = prefs { putString("warProgress", "unstarted") }
        assertNull(IslandWarConcert.nunneryUrl(p))
        assertTrue(IslandWarConcert.concertError("", p).isNotEmpty())
    }
}
