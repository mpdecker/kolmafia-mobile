package net.sourceforge.kolmafia.adventure.choice.solvers

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameproSolverImplTest {

    private fun solver(script: String): GameproSolverImpl {
        val prefs = Preferences(MapSettings())
        prefs.setString("choiceAdventure665", script)
        return GameproSolverImpl(prefs)
    }

    @Test fun emptyPref_returnsNull() = assertNull(solver("").autoSolve(0))
    @Test fun blankPref_returnsNull() = assertNull(solver("  ").autoSolve(0))

    @Test fun script_step0_returnsFirstDigit() =
        assertEquals(2, solver("2,1,3").autoSolve(0))

    @Test fun script_step1_returnsSecondDigit() =
        assertEquals(1, solver("2,1,3").autoSolve(1))

    @Test fun script_step2_returnsThirdDigit() =
        assertEquals(3, solver("2,1,3").autoSolve(2))

    @Test fun stepPastEnd_returnsNull() =
        assertNull(solver("2,1").autoSolve(2))

    @Test fun scriptWithSpaces_parsedCorrectly() =
        assertEquals(4, solver(" 4 , 2 , 1 ").autoSolve(0))
}
