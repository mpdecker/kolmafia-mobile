package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ConcoctionDatabase

class GameRuntimeLibraryAshP541Test {

    @BeforeTest
    fun setUp() {
        ConcoctionDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun revision_phase544() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun budget_bare_printsRemainingAndBudgeted() {
        ConcoctionDatabase.setPullsRemaining(5)
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("budget");""")
        assertTrue(out.contains("5 pulls budgeted for automatic use, 5 pulls remaining."))
    }

    @Test
    fun budget_setsPullsBudgeted() {
        ConcoctionDatabase.setPullsRemaining(10)
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("budget 3");""")
        assertEquals(3, ConcoctionDatabase.getPullsBudgeted())
        assertTrue(out.contains("3 pulls budgeted for automatic use, 10 pulls remaining."))
    }

    @Test
    fun help_listsBudget() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help budget");""")
        assertTrue(out.lines().any { it.trim() == "budget" })
    }
}
