package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP264Test {

    @Test
    fun revision_isphase245() {
        assertEquals("phase485", GameRuntimeLibrary.REVISION)
    }
}
