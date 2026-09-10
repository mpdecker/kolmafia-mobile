package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5770Test {
    @Test
    fun revision_phase5770() {
        assertEquals("phase6670", GameRuntimeLibrary.REVISION)
        assertEquals("6670", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
