package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5530Test {
    @Test
    fun revision_phase5530() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
        assertEquals("6850", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
