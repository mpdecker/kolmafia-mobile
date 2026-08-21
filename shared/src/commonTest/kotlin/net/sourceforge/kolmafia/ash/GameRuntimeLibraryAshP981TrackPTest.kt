package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP981TrackPTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun phase981_isUnrestricted_string() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("true", outputLib(lib, """print(is_unrestricted("anything"));"""))
    }

    @Test
    fun phase982_isTrendy() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("true", outputLib(lib, """print(is_trendy(to_item("none")));"""))
    }

    @Test
    fun phase983_isShruggable() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("true", outputLib(lib, """print(is_shruggable(to_effect("none")));"""))
    }

    @Test
    fun phase984_isGoal_noGoals() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("false", outputLib(lib, """print(is_goal(to_item("none")));"""))
    }
}
