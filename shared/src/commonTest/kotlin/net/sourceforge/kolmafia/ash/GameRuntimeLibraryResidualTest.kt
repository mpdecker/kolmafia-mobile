package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.session.ActionBarManager

class GameRuntimeLibraryResidualTest {
    @AfterTest
    fun tearDown() {
        ActionBarManager.reset()
    }

    @Test
    fun actionbarCliPrintsCachedHeadlessState() {
        ActionBarManager.update("""{"bars":[{"name":"combat"}]}""")

        assertEquals(
            """{"bars":[{"name":"combat"}]}""",
            outputLib(GameRuntimeLibrary(), """cli_execute("actionbar status");"""),
        )

    }
}
