package net.sourceforge.kolmafia.adventure.choice.solvers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SafetyShelterSolverImplTest {

    private val solver = SafetyShelterSolverImpl()

    @Test fun ronald_prefZero_returnsNull() = assertNull(solver.autoRonald(0, 0, ""))
    @Test fun grimace_prefZero_returnsNull() = assertNull(solver.autoGrimace(0, 0, ""))
    @Test fun ronald_prefOutOfRange_returnsNull() = assertNull(solver.autoRonald(7, 0, ""))

    // Ronald goal 1, script "11211"
    @Test fun ronald_goal1_step0_returns1() = assertEquals(1, solver.autoRonald(1, 0, ""))
    @Test fun ronald_goal1_step1_returns1() = assertEquals(1, solver.autoRonald(1, 1, ""))
    @Test fun ronald_goal1_step2_returns2() = assertEquals(2, solver.autoRonald(1, 2, ""))
    @Test fun ronald_goal1_step3_returns1() = assertEquals(1, solver.autoRonald(1, 3, ""))
    @Test fun ronald_goal1_step4_returns1() = assertEquals(1, solver.autoRonald(1, 4, ""))
    @Test fun ronald_goal1_stepExhausted_returnsNull() = assertNull(solver.autoRonald(1, 5, ""))

    // Ronald goal 2, script "1122"
    @Test fun ronald_goal2_step2_returns2() = assertEquals(2, solver.autoRonald(2, 2, ""))

    // Ronald goal 6, script "1322"
    @Test fun ronald_goal6_step1_returns3() = assertEquals(3, solver.autoRonald(6, 1, ""))

    // Grimace goal 1, script "1121"
    @Test fun grimace_goal1_step2_returns2() = assertEquals(2, solver.autoGrimace(1, 2, ""))

    // Grimace goal 4, script "12121"
    @Test fun grimace_goal4_step1_returns2() = assertEquals(2, solver.autoGrimace(4, 1, ""))
    @Test fun grimace_goal4_step4_returns1() = assertEquals(1, solver.autoGrimace(4, 4, ""))
}
