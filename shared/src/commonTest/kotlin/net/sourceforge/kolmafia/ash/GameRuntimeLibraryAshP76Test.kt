package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP76Test {

    @Test
    fun revision_phase120() {
        assertEquals("phase181", GameRuntimeLibrary.REVISION)
    }
}
