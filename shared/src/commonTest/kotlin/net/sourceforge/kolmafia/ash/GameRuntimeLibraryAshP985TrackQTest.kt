package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP985TrackQTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun phase985_haveShop_defaultFalse() {
        val lib = GameRuntimeLibrary(character = KoLCharacter(), preferences = prefs())
        assertEquals("false", outputLib(lib, "print(have_shop());"))
    }

    @Test
    fun phase985_haveDisplay_defaultFalse() {
        val lib = GameRuntimeLibrary(character = KoLCharacter(), preferences = prefs())
        assertEquals("false", outputLib(lib, "print(have_display());"))
    }

    @Test
    fun phase989_dailySpecial_default() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("", outputLib(lib, "print(daily_special());"))
    }

    @Test
    fun phase990_sellsSkill_defaultFalse() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("false", outputLib(lib, """print(sells_skill(to_coinmaster("none")));"""))
    }
}
