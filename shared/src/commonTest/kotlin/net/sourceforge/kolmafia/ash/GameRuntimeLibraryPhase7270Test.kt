package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.session.TcrsCliManager

/**
 * TCRS full derive sweep wrap (phases 7211–7270).
 * Parent wrap bumps REVISION to phase7330.
 */
class GameRuntimeLibraryphase7330Test {

    @Test
    fun revision_isphase7330() {
        assertEquals("phase7510", GameRuntimeLibrary.REVISION)
        assertEquals("7510", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun helpTcrs_listsTheVerbAndDoesNotMarkDumpsUnavailable() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help tcrs");""")
        assertTrue("tcrs" in out.lowercase(), out)
        assertFalse(out.contains("TCRS dumps"), out)
    }

    @Test
    fun tcrsHelp_includesIntrospectAndUpdate() {
        val lib = GameRuntimeLibrary(tcrsCliManager = TcrsCliManager())
        val out = outputLib(lib, """cli_execute("tcrs help");""")
        assertTrue(out.contains("introspect"), out)
        assertTrue(out.contains("update"), out)
        assertTrue(out.contains("derive"), out)
    }
}
