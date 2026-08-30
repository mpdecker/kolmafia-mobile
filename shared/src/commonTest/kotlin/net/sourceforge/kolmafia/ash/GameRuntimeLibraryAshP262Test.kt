package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP262Test {

    @Test
    fun revision_phase246() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun userNotify_oneArg_returnsVoid() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("", outputLib(lib, """user_notify("hi");""").trim())
    }

    @Test
    fun userNotify_twoArg_returnsVoid() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("", outputLib(lib, """user_notify("hi", true);""").trim())
    }
}
