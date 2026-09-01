package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoalManagerFactoidTest {
    @Test
    fun noteFactoidLearned_returnsTrueWhenCountGoalCompletes() {
        val goals = GoalManager()
        goals.setFactoidCountGoal(2)
        assertFalse(goals.noteFactoidLearned())
        assertTrue(goals.hasFactoidCountGoal())
        assertTrue(goals.noteFactoidLearned())
        assertFalse(goals.hasFactoidCountGoal())
    }
}
