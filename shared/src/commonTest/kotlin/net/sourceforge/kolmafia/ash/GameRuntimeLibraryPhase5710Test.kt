package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5710Test {
    @Test
    fun revision_phase5710() {
        assertEquals("phase5950", GameRuntimeLibrary.REVISION)
        assertEquals("phase5950", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
