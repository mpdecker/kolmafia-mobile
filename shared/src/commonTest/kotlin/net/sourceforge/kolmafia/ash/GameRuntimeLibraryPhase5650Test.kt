package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5650Test {
    @Test
    fun revision_phase5650() {
        assertEquals("phase7510", GameRuntimeLibrary.REVISION)
        assertEquals("7510", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
