package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP80Test {

    @Test
    fun revision_phase136() {
        assertEquals("phase136", GameRuntimeLibrary.REVISION)
    }
}
