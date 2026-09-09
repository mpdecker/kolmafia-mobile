package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5650Test {
    @Test
    fun revision_phase5650() {
        assertEquals("phase5890", GameRuntimeLibrary.REVISION)
        assertEquals("phase5890", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
