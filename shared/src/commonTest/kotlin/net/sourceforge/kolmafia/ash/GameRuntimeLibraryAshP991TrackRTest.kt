package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP991TrackRTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun phase991_extractMeat() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val result = outputLib(lib, """print(extract_meat("You gain 1,234 Meat"));""")
        assertEquals("1234", result)
    }

    @Test
    fun phase992_lastEncounter() {
        val p = prefs { putString("lastEncounter", "Knob Goblin") }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("Knob Goblin", outputLib(lib, "print(last_encounter());"))
    }

    @Test
    fun phase993_attack_returnsMacro() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val result = outputLib(lib, """print(attack());""")
        assertEquals("attack", result)
    }

    @Test
    fun phase995_pickpocket_returnsMacro() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("pickpocket", outputLib(lib, """print(pickpocket());"""))
    }
}
