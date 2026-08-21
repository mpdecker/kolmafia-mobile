package net.sourceforge.kolmafia.ash

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP512Test {

    @BeforeTest
    fun setUp() = runBlocking {
        GameDatabase().load()
    }

    @Test
    fun revision_phase512() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun safe_printsAreaSummary() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("safe The Haunted Pantry");""")
        assertTrue(out.contains("Hit:"))
        assertTrue(out.contains("Combat Rate:"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun safe_unknownLocation_isSilent() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("safe zzznosuchlocation999");""")
        assertEquals("", out.trim())
    }
}
