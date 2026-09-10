package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5830Test {
    @Test
    fun revision_phase6310() {
        assertEquals("phase6370", GameRuntimeLibrary.REVISION)
        assertEquals("phase6370", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
