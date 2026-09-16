package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPhase5890Test {
    @Test
    fun revision_phase6310() {
        assertEquals("phase7510", GameRuntimeLibrary.REVISION)
        assertEquals("7510", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }
}
