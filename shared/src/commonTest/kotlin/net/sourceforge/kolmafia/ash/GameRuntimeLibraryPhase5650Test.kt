package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5650Test {
    @Test
    fun revision_phase5650() {
        assertEquals("phase5950", GameRuntimeLibrary.REVISION)
        assertEquals("phase5950", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
