package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP997TrackSTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun phase997_myMaxfury() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("3", outputLib(lib, "print(my_maxfury());"))
    }

    @Test
    fun phase997_myWildfireWater() {
        val p = prefs { putInt("wildfireWater", 50) }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("50", outputLib(lib, "print(my_wildfire_water());"))
    }

    @Test
    fun phase998_minstrelLevel() {
        val p = prefs { putInt("clancyLevel", 7) }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("7", outputLib(lib, "print(minstrel_level());"))
    }

    @Test
    fun phase998_minstrelQuest() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("false", outputLib(lib, "print(minstrel_quest());"))
    }
}
