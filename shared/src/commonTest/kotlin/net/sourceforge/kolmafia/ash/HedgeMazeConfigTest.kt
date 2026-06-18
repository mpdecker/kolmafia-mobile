package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class HedgeMazeConfigTest {

    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    @Test
    fun estimateMazeTurns_trapsFromRoom0_countsNineTurns() {
        val p = prefs {
            setInt("choiceAdventure1005", 2)
            setInt("choiceAdventure1006", 1)
            setInt("choiceAdventure1007", 1)
            setInt("choiceAdventure1008", 2)
            setInt("choiceAdventure1009", 1)
            setInt("choiceAdventure1010", 1)
            setInt("choiceAdventure1011", 2)
            setInt("choiceAdventure1012", 1)
            setInt("choiceAdventure1013", 1)
        }
        assertEquals(4, estimateMazeTurns(p, 0))
    }

    @Test
    fun estimateMazeTurns_gopherFromMidRoom_countsRemainingTurns() {
        val p = prefs {
            setInt("choiceAdventure1005", 1)
            setInt("choiceAdventure1006", 2)
            setInt("choiceAdventure1007", 1)
            setInt("choiceAdventure1008", 1)
            setInt("choiceAdventure1009", 2)
            setInt("choiceAdventure1010", 1)
            setInt("choiceAdventure1011", 1)
            setInt("choiceAdventure1012", 1)
            setInt("choiceAdventure1013", 1)
        }
        assertEquals(5, estimateMazeTurns(p, 4))
    }
}
