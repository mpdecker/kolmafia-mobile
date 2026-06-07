package net.sourceforge.kolmafia.adventure.choice.solvers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LostKeySolverImplTest {

    private val solver = LostKeySolverImpl()

    // Glasses script "121111"
    @Test fun glasses_step0_returns1() = assertEquals(1, solver.autoKey(1, 0, ""))
    @Test fun glasses_step1_returns2() = assertEquals(2, solver.autoKey(1, 1, ""))
    @Test fun glasses_step2_returns1() = assertEquals(1, solver.autoKey(1, 2, ""))
    @Test fun glasses_step5_returns1() = assertEquals(1, solver.autoKey(1, 5, ""))
    @Test fun glasses_step6_returnsNull() = assertNull(solver.autoKey(1, 6, ""))

    // Comb script "131212"
    @Test fun comb_step1_returns3() = assertEquals(3, solver.autoKey(2, 1, ""))
    @Test fun comb_step2_returns1() = assertEquals(1, solver.autoKey(2, 2, ""))
    @Test fun comb_step3_returns2() = assertEquals(2, solver.autoKey(2, 3, ""))

    // Pill bottle script "131113"
    @Test fun pillBottle_step3_returns1() = assertEquals(1, solver.autoKey(3, 3, ""))
    @Test fun pillBottle_step5_returns3() = assertEquals(3, solver.autoKey(3, 5, ""))

    // Out of range
    @Test fun prefZero_returnsNull() = assertNull(solver.autoKey(0, 0, ""))
    @Test fun prefFour_returnsNull() = assertNull(solver.autoKey(4, 0, ""))
}
