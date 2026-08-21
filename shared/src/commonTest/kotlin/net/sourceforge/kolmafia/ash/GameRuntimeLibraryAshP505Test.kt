package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP505Test {

    @Test
    fun revision_phase505() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun condref_listsStatDayAndClassConditions() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("condref");""")
        assertTrue(out.contains("today", ignoreCase = true))
        assertTrue(out.contains("tomorrow", ignoreCase = true))
        assertTrue(out.contains("class is", ignoreCase = true))
        assertTrue(out.contains("skill list", ignoreCase = true))
    }

    @Test
    fun condref_listsNumericLvaluesAndOperators() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("condref");""")
        assertTrue(out.contains("level", ignoreCase = true))
        assertTrue(out.contains("health", ignoreCase = true))
        assertTrue(out.contains("mana", ignoreCase = true))
        assertTrue(out.contains("meat", ignoreCase = true))
        assertTrue(out.contains("adventures", ignoreCase = true))
        assertTrue(out.contains("inebriety", ignoreCase = true))
        assertTrue(out.contains("worthless item", ignoreCase = true))
        assertTrue(out.contains("stickers", ignoreCase = true))
        assertTrue(out.contains(">="))
        assertTrue(out.contains("%"))
    }
}
