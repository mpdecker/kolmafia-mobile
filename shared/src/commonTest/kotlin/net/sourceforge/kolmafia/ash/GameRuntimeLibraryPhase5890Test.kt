package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5890Test {
    @Test
    fun revision_phase6310() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
        assertEquals("6850", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
