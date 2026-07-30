package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP132Test {

    @Test
    fun revision_isphase170() {
        assertEquals("phase240", GameRuntimeLibrary.REVISION)
    }
}
