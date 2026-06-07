package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.GoalManager
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryGoalsTest {

    @Test
    fun addItemCondition_thenGoalExistsItem() {
        val gm = GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        outputLib(lib, "add_item_condition(1, to_item(\"seal tooth\"));")
        assertEquals("true", outputLib(lib, "print(to_string(goal_exists(\"item\")));"))
    }

    @Test
    fun removeItemCondition_thenGoalExistsFalse() {
        val gm = GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        outputLib(lib, "add_item_condition(1, to_item(\"seal tooth\"));")
        outputLib(lib, "remove_item_condition(1, to_item(\"seal tooth\"));")
        assertEquals("false", outputLib(lib, "print(to_string(goal_exists(\"item\")));"))
    }

    @Test
    fun getGoals_countMatchesAdded() {
        val gm = GoalManager()
        gm.addItemGoalByName("seal tooth")
        gm.addItemGoalByName("spooky sprocket")
        val lib = GameRuntimeLibrary(goalManager = gm)
        assertEquals("2", outputLib(lib, "print(to_string(count(get_goals())));"))
    }

    @Test
    fun goalExists_meat_falseWhenNotSet() {
        val gm = GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        assertEquals("false", outputLib(lib, "print(to_string(goal_exists(\"meat\")));"))
    }

    @Test
    fun goalExists_meat_trueWhenSet() {
        val gm = GoalManager()
        gm.setMeatGoal(1000)
        val lib = GameRuntimeLibrary(goalManager = gm)
        assertEquals("true", outputLib(lib, "print(to_string(goal_exists(\"meat\")));"))
    }

    @Test
    fun goalExists_level_trueWhenSet() {
        val gm = GoalManager()
        gm.setLevelGoal(10)
        val lib = GameRuntimeLibrary(goalManager = gm)
        assertEquals("true", outputLib(lib, "print(to_string(goal_exists(\"level\")));"))
    }
}
