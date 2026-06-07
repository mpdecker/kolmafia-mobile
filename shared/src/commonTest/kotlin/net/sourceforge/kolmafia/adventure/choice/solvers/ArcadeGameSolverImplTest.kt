package net.sourceforge.kolmafia.adventure.choice.solvers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArcadeGameSolverImplTest {

    private val solver = ArcadeGameSolverImpl()

    // FistScript[0] = '3'
    @Test fun step0_noMemory_returns3() =
        assertEquals(3, solver.autoDungeonFist(0, "no finish from memory here"))

    // FistScript[1] = '1'
    @Test fun step1_noMemory_returns1() =
        assertEquals(1, solver.autoDungeonFist(1, "no finish from memory here"))

    // Spot checks
    @Test fun step59_returns1() = assertEquals(1, solver.autoDungeonFist(59, ""))
    @Test fun step119_returns3() = assertEquals(3, solver.autoDungeonFist(119, ""))

    // Past end of script → null
    @Test fun stepPastEnd_returnsNull() = assertNull(solver.autoDungeonFist(120, ""))
    @Test fun negativeStep_returnsNull() = assertNull(solver.autoDungeonFist(-1, ""))

    // "Finish from Memory" present → return that option number
    @Test fun finishFromMemoryPresent_returnsItsOption() {
        val html = """<form><input name="whichchoice" value="486">
            <a href="choice.php?option=3">Finish from Memory</a>
            <a href="choice.php?option=1">Keep going</a></form>"""
        assertEquals(3, solver.autoDungeonFist(5, html))
    }

    // "Finish from Memory" at step 0 — shortcut still wins
    @Test fun finishFromMemoryAtStep0_returnsItsOption() {
        val html = """option=2">Finish from Memory</a>"""
        assertEquals(2, solver.autoDungeonFist(0, html))
    }
}
