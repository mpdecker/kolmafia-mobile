package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP75Test {

    @Test
    fun revision_phase120() {
        assertEquals("phase220", GameRuntimeLibrary.REVISION)
    }
}
