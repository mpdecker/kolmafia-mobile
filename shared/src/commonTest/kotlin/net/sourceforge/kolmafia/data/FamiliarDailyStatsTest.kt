package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences

class FamiliarDailyStatsTest {

    @Test
    fun greenPixieDropCapAndTracker() {
        val info = FamiliarDailyStats.getDropInfo(70)
        assertEquals("absinthe", info?.dropName)
        assertEquals(2655, info?.dropItemId)
        assertEquals(5, FamiliarDailyStats.dropDailyCap(70, null))
    }

    @Test
    fun dropsToday_readsPrefCounter() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_absintheDrops", 3)
        assertEquals(3, FamiliarDailyStats.dropsToday(70, prefs))
    }

    @Test
    fun hipsterFightCap() {
        assertEquals(7, FamiliarDailyStats.fightDailyCap(136))
        val prefs = Preferences(MapSettings())
        prefs.setInt("_hipsterAdv", 2)
        assertEquals(2, FamiliarDailyStats.fightsToday(136, prefs))
    }
}
