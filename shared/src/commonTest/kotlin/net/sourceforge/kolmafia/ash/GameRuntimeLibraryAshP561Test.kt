package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP561Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun help_listsSilentConsumeAndCraftAliases() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help");""")
        val lines = out.lines().map { it.trim() }
        for (name in listOf(
            "eatsilent", "drinksilent", "overdrink",
            "bake", "mix", "smith", "tinker", "ply",
            "eatqueue", "drinkqueue", "chewqueue", "usequeue", "createqueue",
            "ghostqueue", "hoboqueue", "slimelingqueue", "roboequeue",
        )) {
            assertTrue(lines.any { it == name }, "missing help entry: $name")
        }
    }
}
