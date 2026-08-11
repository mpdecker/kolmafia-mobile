package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP425Test {

    @Test
    fun revision_phase440() {
        assertEquals("phase440", GameRuntimeLibrary.REVISION)
    }
}
