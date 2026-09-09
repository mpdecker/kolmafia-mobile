package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5530Test {
    @Test
    fun revision_phase5530() {
        assertEquals("phase5890", GameRuntimeLibrary.REVISION)
        assertEquals("phase5890", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
