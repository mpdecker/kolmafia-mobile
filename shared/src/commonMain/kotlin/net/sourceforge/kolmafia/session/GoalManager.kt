package net.sourceforge.kolmafia.session

/**
 * Tracks acquisition goals for the current automation session.
 * Supports item goals by ID, item goals by name (case-insensitive), meat goals, and level goals.
 */
class GoalManager {
    private val _itemGoalIds   = mutableSetOf<Int>()
    private val _itemGoalNames = mutableSetOf<String>()  // stored lowercase+trimmed
    private var meatGoal: Int?  = null
    private var levelGoal: Int? = null

    // ── ID-based item goals ───────────────────────────────────────────────────

    fun addItemGoal(itemId: Int)    { _itemGoalIds.add(itemId) }
    fun removeItemGoal(itemId: Int) { _itemGoalIds.remove(itemId) }
    fun hasItemGoal(itemId: Int): Boolean = _itemGoalIds.contains(itemId)
    fun itemGoalIds(): Set<Int> = _itemGoalIds.toSet()

    // ── Name-based item goals (case-insensitive) ──────────────────────────────

    fun addItemGoalByName(name: String)    { _itemGoalNames.add(name.lowercase().trim()) }
    fun removeItemGoalByName(name: String) { _itemGoalNames.remove(name.lowercase().trim()) }
    fun hasItemGoalByName(name: String): Boolean = _itemGoalNames.contains(name.lowercase().trim())

    // ── Meat goal ─────────────────────────────────────────────────────────────

    fun setMeatGoal(meat: Int)  { meatGoal = meat }
    fun clearMeatGoal()         { meatGoal = null }
    /** Returns true when [currentMeat] meets or exceeds the configured meat goal. */
    fun hasMeatGoal(currentMeat: Int): Boolean = meatGoal?.let { currentMeat >= it } ?: false

    // ── Level goal ────────────────────────────────────────────────────────────

    fun setLevelGoal(level: Int)  { levelGoal = level }
    fun clearLevelGoal()          { levelGoal = null }
    /** Returns true when [currentLevel] meets or exceeds the configured level goal. */
    fun hasLevelGoal(currentLevel: Int): Boolean = levelGoal?.let { currentLevel >= it } ?: false

    // ── Clear all ─────────────────────────────────────────────────────────────

    fun clearGoals() {
        _itemGoalIds.clear()
        _itemGoalNames.clear()
        meatGoal = null
        levelGoal = null
    }
}
