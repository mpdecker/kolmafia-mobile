package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP80Test {

    @Test
    fun revision_phase141() {
        assertEquals("phase421", GameRuntimeLibrary.REVISION)
    }
}
