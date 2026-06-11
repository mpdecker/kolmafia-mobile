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
    private var factoidGoal: String? = null

    // ── Factoid goal (response text match) ────────────────────────────────────

    fun setFactoidGoal(text: String) { factoidGoal = text.trim().takeIf { it.isNotBlank() } }
    fun clearFactoidGoal() { factoidGoal = null }
    fun hasFactoidGoalSet(): Boolean = factoidGoal != null
    fun matchesFactoid(responseText: String): Boolean {
        val goal = factoidGoal ?: return false
        return responseText.contains(goal, ignoreCase = true)
    }

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

    // ── Phase 10 ASH helpers ──────────────────────────────────────────────────

    /** True if any item goal (by ID or name) is active. */
    fun hasItemGoals(): Boolean = _itemGoalIds.isNotEmpty() || _itemGoalNames.isNotEmpty()

    /** True if a meat goal has been set (regardless of current meat). */
    fun hasMeatGoalSet(): Boolean = meatGoal != null

    /** True if a level goal has been set (regardless of current level). */
    fun hasLevelGoalSet(): Boolean = levelGoal != null

    fun hasFactoidGoal(): Boolean = factoidGoal != null

    /** Remove the first item goal matching [itemName] (case-insensitive). */
    fun removeGoal(itemName: String) { removeItemGoalByName(itemName) }

    /** Serialize all active goals as human-readable strings. */
    fun allGoalsAsStrings(): List<String> = buildList {
        _itemGoalIds.forEach  { add("item id:$it") }
        _itemGoalNames.forEach { add("item name:$it") }
        meatGoal?.let  { add("meat:$it") }
        levelGoal?.let { add("level:$it") }
        factoidGoal?.let { add("factoid:$it") }
    }

    // ── Clear all ─────────────────────────────────────────────────────────────

    fun clearGoals() {
        _itemGoalIds.clear()
        _itemGoalNames.clear()
        meatGoal = null
        levelGoal = null
        factoidGoal = null
    }
}
