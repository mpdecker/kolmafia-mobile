package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP80Test {

    @Test
    fun revision_phase126() {
        assertEquals("phase126", GameRuntimeLibrary.REVISION)
    }
}
