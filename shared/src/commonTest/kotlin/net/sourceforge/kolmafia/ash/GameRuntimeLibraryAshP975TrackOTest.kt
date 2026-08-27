package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP975TrackOTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun phase975_containsText() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("true", outputLib(lib, """print(contains_text("Hello World", "hello"));"""))
    }

    @Test
    fun phase975_startsWith() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("true", outputLib(lib, """print(starts_with("Hello World", "Hello"));"""))
        assertEquals("false", outputLib(lib, """print(starts_with("Hello World", "World"));"""))
    }

    @Test
    fun phase975_endsWith() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("true", outputLib(lib, """print(ends_with("Hello World", "World"));"""))
    }

    @Test
    fun phase980_isInteger() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("true", outputLib(lib, """print(is_integer("42"));"""))
        assertEquals("false", outputLib(lib, """print(is_integer("abc"));"""))
    }

    @Test
    fun phase980_isFloat() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("true", outputLib(lib, """print(is_float("3.14"));"""))
        assertEquals("false", outputLib(lib, """print(is_float("xyz"));"""))
    }
}
