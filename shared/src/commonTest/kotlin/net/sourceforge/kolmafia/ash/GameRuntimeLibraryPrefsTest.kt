package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPrefsTest {

    @Test
    fun getProperty_unknownKeyReturnsEmpty() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("", outputLib(lib, "print(get_property(\"noSuchKey\"));"))
    }

    @Test
    fun setAndGetProperty_roundTrips() {
        // Both outputLib calls use the same lib (and therefore same Preferences instance)
        // so state is shared across ASH invocations.
        val lib = GameRuntimeLibrary(preferences = prefs())
        outputLib(lib, "set_property(\"myKey\", \"myValue\");")
        assertEquals("myValue", outputLib(lib, "print(get_property(\"myKey\"));"))
    }

    @Test
    fun setProperty_overwritesPreviousValue() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        outputLib(lib, "set_property(\"k\", \"first\");")
        outputLib(lib, "set_property(\"k\", \"second\");")
        assertEquals("second", outputLib(lib, "print(get_property(\"k\"));"))
    }

    @Test
    fun getProperty_noPreferencesReturnsEmpty() {
        val lib = GameRuntimeLibrary() // no preferences injected
        assertEquals("", outputLib(lib, "print(get_property(\"anything\"));"))
    }
}
