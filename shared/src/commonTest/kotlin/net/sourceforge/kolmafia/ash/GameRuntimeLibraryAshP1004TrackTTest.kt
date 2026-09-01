package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP1004TrackTTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun phase1004_nowToInt_positive() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val result = outputLib(lib, "print(now_to_int());").toLong()
        assertTrue(result > 0, "now_to_int should return positive millis")
    }

    @Test
    fun phase1004_gametimeToInt_positive() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val result = outputLib(lib, "print(gametime_to_int());").toLong()
        assertTrue(result > 0, "gametime_to_int should return positive millis")
    }

    @Test
    fun phase1005_moonPhase_inRange() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val result = outputLib(lib, "print(moon_phase());").toInt()
        assertTrue(result in 0..7, "moon_phase should be 0-7")
    }

    @Test
    fun phase1005_moonLight_inRange() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val result = outputLib(lib, "print(moon_light());").toInt()
        assertTrue(result in 0..8, "moon_light should be 0-8")
    }

    @Test
    fun phase1007_currentMaximizerScore() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0.0", outputLib(lib, "print(current_maximizer_score());"))
    }

    @Test
    fun phase1008_tavern_defaultZero() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, "print(tavern());"))
    }

    @Test
    fun phase1008_tavern_withLayout() {
        val p = prefs { putString("tavernLayout", "1230") }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("3", outputLib(lib, "print(tavern());"))
    }

    @Test
    fun phase1010_revision() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun phase1010_favoriteFamiliars_empty() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, "print(count(favorite_familiars()));"))
    }
}
