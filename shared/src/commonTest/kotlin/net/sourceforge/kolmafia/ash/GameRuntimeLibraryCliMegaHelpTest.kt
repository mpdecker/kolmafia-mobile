package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phases 1063–1070 help promote + revision corpus. */
class GameRuntimeLibraryCliMegaHelpTest {

    @Test
    fun revision_isPhase1070() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun help_listsPromotedLiveVerbs() {
        val lib = GameRuntimeLibrary()
        for (verb in listOf("olfact", "putty", "squeeze", "shrug", "saber", "snapper", "autumnaton", "cmc", "heist", "tavern", "undercut", "skeeball")) {
            val out = outputLib(lib, """cli_execute("help $verb");""")
            assertTrue(out.contains(verb), "help missing $verb: $out")
        }
    }

    @Test
    fun help_listsTrackBFamiliarPath() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("help absorptions");""")
        assertTrue(out.contains("absorptions"), out)
        val out2 = outputLib(lib, """cli_execute("help bugbears");""")
        assertTrue(out2.contains("bugbears"), out2)
    }
}
