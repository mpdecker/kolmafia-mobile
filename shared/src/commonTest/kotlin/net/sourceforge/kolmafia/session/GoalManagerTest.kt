package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoalManagerTest {

    @Test fun hasItemGoal_afterAdd_returnsTrue() {
        val gm = GoalManager()
        gm.addItemGoal(42)
        assertTrue(gm.hasItemGoal(42))
    }

    @Test fun hasItemGoal_notAdded_returnsFalse() {
        assertFalse(GoalManager().hasItemGoal(42))
    }

    @Test fun removeItemGoal_removesIt() {
        val gm = GoalManager()
        gm.addItemGoal(42)
        gm.removeItemGoal(42)
        assertFalse(gm.hasItemGoal(42))
    }

    @Test fun clearGoals_removesAll() {
        val gm = GoalManager()
        gm.addItemGoal(1); gm.addItemGoal(2)
        gm.clearGoals()
        assertFalse(gm.hasItemGoal(1))
        assertFalse(gm.hasItemGoal(2))
    }
}
