package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP527Test {

    @Test
    fun revision_phase527() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun help_wait_listsWait() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help wait");""")
        assertTrue(out.lines().any { it.trim() == "wait" })
        assertTrue(out.lines().any { it.trim() == "waitq" })
    }

    @Test
    fun which_bounty_listsBounty() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("which bounty");""")
        assertTrue(out.lines().any { it.trim() == "bounty" })
    }

    @Test
    fun help_promotesStashRecipeRecover() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help");""")
        for (name in listOf("stash", "recipe", "ingredients", "recover", "send", "retrieve", "absorb")) {
            assertTrue(out.lines().any { it.trim() == name }, "expected $name in help")
        }
    }
}
