package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP135Test {

    @Test
    fun revision_phase170() {
        assertEquals("phase220", GameRuntimeLibrary.REVISION)
    }
}
