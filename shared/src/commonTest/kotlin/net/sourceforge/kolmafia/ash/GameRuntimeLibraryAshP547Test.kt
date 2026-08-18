package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP547Test {

    @Test
    fun revision_phase550() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun help_listsPromotedCommands() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help");""")
        val lines = out.lines().map { it.trim() }
        for (name in listOf(
            "umbrella",
            "parka",
            "horsery",
            "boombox",
            "mcd",
            "enthrone",
            "bjornify",
            "cargo",
            "pulverize",
            "stickers",
            "folders",
            "timeout",
            "journey",
            "witchess",
            "volcano",
            "aa",
            "hagnk",
            "make",
        )) {
            assertTrue(lines.any { it == name }, "expected $name in help")
        }
    }
}
