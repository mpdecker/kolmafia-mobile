package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5590Test {
    @Test
    fun revision_phase5590() {
        assertEquals("phase6370", GameRuntimeLibrary.REVISION)
        assertEquals("phase6370", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
