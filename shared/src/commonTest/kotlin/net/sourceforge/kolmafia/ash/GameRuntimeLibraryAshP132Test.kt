package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP132Test {

    @Test
    fun revision_isphase170() {
        assertEquals("phase200", GameRuntimeLibrary.REVISION)
    }
}
