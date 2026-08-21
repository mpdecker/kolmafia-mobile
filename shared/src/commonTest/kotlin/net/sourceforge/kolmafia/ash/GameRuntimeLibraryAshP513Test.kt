package net.sourceforge.kolmafia.ash

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP513Test {

    @BeforeTest
    fun setUp() = runBlocking {
        GameDatabase().load()
    }

    @Test
    fun revision_phase513() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun monsters_printsPerMonsterDump() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("monsters The Haunted Pantry");""")
        assertTrue(out.contains("possessed can of tomatoes"))
        assertTrue(out.contains("Atk:"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun monsters_unknownLocation_isSilent() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("monsters zzznosuchlocation999");""")
        assertEquals("", out.trim())
    }
}
