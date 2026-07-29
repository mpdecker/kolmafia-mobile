package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP130Test {

    @Test
    fun revision_isphase170() {
        assertEquals("phase220", GameRuntimeLibrary.REVISION)
    }
}
