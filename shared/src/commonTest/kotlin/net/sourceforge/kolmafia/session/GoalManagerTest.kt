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

    // ── Name-based item goals ──────────────────────────────────────────────────

    @Test fun hasItemGoalByName_afterAdd_returnsTrue() {
        val gm = GoalManager()
        gm.addItemGoalByName("Knob Goblin Lasso")
        assertTrue(gm.hasItemGoalByName("Knob Goblin Lasso"))
    }

    @Test fun hasItemGoalByName_caseInsensitive() {
        val gm = GoalManager()
        gm.addItemGoalByName("Knob Goblin Lasso")
        assertTrue(gm.hasItemGoalByName("knob goblin lasso"))
        assertTrue(gm.hasItemGoalByName("KNOB GOBLIN LASSO"))
    }

    @Test fun hasItemGoalByName_leadingTrailingWhitespace_matches() {
        val gm = GoalManager()
        gm.addItemGoalByName("  rat whisker  ")
        assertTrue(gm.hasItemGoalByName("rat whisker"))
    }

    @Test fun removeItemGoalByName_removesIt() {
        val gm = GoalManager()
        gm.addItemGoalByName("Meat Vortex")
        gm.removeItemGoalByName("Meat Vortex")
        assertFalse(gm.hasItemGoalByName("Meat Vortex"))
    }

    @Test fun hasItemGoalByName_notAdded_returnsFalse() {
        assertFalse(GoalManager().hasItemGoalByName("nothing"))
    }

    // ── Meat goal ──────────────────────────────────────────────────────────

    @Test fun hasMeatGoal_atGoal_returnsTrue() {
        val gm = GoalManager()
        gm.setMeatGoal(10_000)
        assertTrue(gm.hasMeatGoal(10_000))
    }

    @Test fun hasMeatGoal_aboveGoal_returnsTrue() {
        val gm = GoalManager()
        gm.setMeatGoal(10_000)
        assertTrue(gm.hasMeatGoal(15_000))
    }

    @Test fun hasMeatGoal_belowGoal_returnsFalse() {
        val gm = GoalManager()
        gm.setMeatGoal(10_000)
        assertFalse(gm.hasMeatGoal(9_999))
    }

    @Test fun hasMeatGoal_noGoalSet_returnsFalse() {
        assertFalse(GoalManager().hasMeatGoal(99_999))
    }

    @Test fun clearMeatGoal_clearsIt() {
        val gm = GoalManager()
        gm.setMeatGoal(5_000)
        gm.clearMeatGoal()
        assertFalse(gm.hasMeatGoal(99_999))
    }

    // ── Level goal ────────────────────────────────────────────────────────

    @Test fun hasLevelGoal_atGoal_returnsTrue() {
        val gm = GoalManager()
        gm.setLevelGoal(10)
        assertTrue(gm.hasLevelGoal(10))
    }

    @Test fun hasLevelGoal_aboveGoal_returnsTrue() {
        val gm = GoalManager()
        gm.setLevelGoal(10)
        assertTrue(gm.hasLevelGoal(11))
    }

    @Test fun hasLevelGoal_belowGoal_returnsFalse() {
        val gm = GoalManager()
        gm.setLevelGoal(10)
        assertFalse(gm.hasLevelGoal(9))
    }

    @Test fun hasLevelGoal_noGoalSet_returnsFalse() {
        assertFalse(GoalManager().hasLevelGoal(99))
    }

    @Test fun clearLevelGoal_clearsIt() {
        val gm = GoalManager()
        gm.setLevelGoal(10)
        gm.clearLevelGoal()
        assertFalse(gm.hasLevelGoal(99))
    }

    // ── clearGoals clears everything ─────────────────────────────────────

    @Test fun clearGoals_clearsNameGoalsAndNumericGoals() {
        val gm = GoalManager()
        gm.addItemGoal(1)
        gm.addItemGoalByName("Rat Whisker")
        gm.setMeatGoal(5_000)
        gm.setLevelGoal(10)
        gm.clearGoals()
        assertFalse(gm.hasItemGoal(1))
        assertFalse(gm.hasItemGoalByName("Rat Whisker"))
        assertFalse(gm.hasMeatGoal(99_999))
        assertFalse(gm.hasLevelGoal(99))
    }
}
