package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP421Test {

    @Test
    fun revision_phase450() {
        assertEquals("phase450", GameRuntimeLibrary.REVISION)
    }
}
