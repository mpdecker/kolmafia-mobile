package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.request.BeachCombRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameRuntimeLibraryAshP848Test {
    @Test
    fun beachCliParsesDesktopCommandsAndCoordinates() {
        assertEquals(BeachCombRequest.Command.VISIT, BeachCombRequest.parseCommand("visit")?.command)
        assertEquals(420, BeachCombRequest.parseCommand("wander 420")?.minutes)
        assertEquals(8, BeachCombRequest.parseCommand("comb 8 4")?.row)
        assertEquals(4, BeachCombRequest.parseCommand("comb 8 4")?.column)
        assertEquals("3", BeachCombRequest.parseCommand("head 3")?.query)
        assertNull(BeachCombRequest.parseCommand("wander nope"))
        assertNull(BeachCombRequest.parseCommand("comb 8"))
    }
}
