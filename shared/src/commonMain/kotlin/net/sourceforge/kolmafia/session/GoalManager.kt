package net.sourceforge.kolmafia.session

/**
 * Tracks acquisition goals for the current automation session.
 * Mirrors the item-goal subset of the desktop GoalManager.
 */
class GoalManager {
    private val itemGoals = mutableSetOf<Int>()

    fun addItemGoal(itemId: Int)    { itemGoals.add(itemId) }
    fun removeItemGoal(itemId: Int) { itemGoals.remove(itemId) }
    fun hasItemGoal(itemId: Int): Boolean = itemGoals.contains(itemId)
    fun clearGoals() { itemGoals.clear() }

    fun itemGoalIds(): Set<Int> = itemGoals.toSet()
}
