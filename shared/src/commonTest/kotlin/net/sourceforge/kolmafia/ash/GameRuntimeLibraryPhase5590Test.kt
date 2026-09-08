package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5590Test {
    @Test
    fun revision_phase5590() {
        assertEquals("phase5590", GameRuntimeLibrary.REVISION)
        assertEquals("phase5590", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
