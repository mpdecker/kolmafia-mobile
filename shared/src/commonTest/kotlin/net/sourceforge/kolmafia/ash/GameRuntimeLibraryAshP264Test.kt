package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP264Test {

    @Test
    fun revision_isphase245() {
        assertEquals("phase260", GameRuntimeLibrary.REVISION)
    }
}
