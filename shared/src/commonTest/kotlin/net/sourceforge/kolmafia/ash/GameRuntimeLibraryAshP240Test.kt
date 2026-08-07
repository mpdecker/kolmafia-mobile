package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP240Test {

    @Test
    fun revision_isPhase223() {
        assertEquals("phase290", GameRuntimeLibrary.REVISION)
    }
}
