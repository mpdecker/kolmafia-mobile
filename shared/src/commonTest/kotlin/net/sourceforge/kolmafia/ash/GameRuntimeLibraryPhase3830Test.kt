package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

class GameRuntimeLibraryPhase3830Test {

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
    }

    @Test
    fun revision_phase3830() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun availableChoiceOptions_spoilersUseCatalog() {
        ChoiceCombatAshState.lastChoiceResponseText = """
            <form><input type="hidden" name="whichchoice" value="4">
            <input type="hidden" name="option" value="2">Order poultry</form>
        """.trimIndent()
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, "print(available_choice_options(true)[2]);")
        assertTrue(out.contains("poultrygeist"), out)
    }

    @Test
    fun choiceGoalCli_requiresActiveChoice() {
        ChoiceCombatAshState.reset()
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("choice-goal");""")
        assertTrue(out.contains("not currently in a choice"), out)
    }

    @Test
    fun helpListsChoiceGoal() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("help choice-goal");""")
        assertTrue(out.contains("choice-goal"), out)
    }
}
