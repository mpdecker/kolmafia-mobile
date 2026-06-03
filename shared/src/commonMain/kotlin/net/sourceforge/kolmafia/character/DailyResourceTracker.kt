package net.sourceforge.kolmafia.character

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Tracks per-day use counts for items and free actions that are not directly
// surfaced by the KoL API. Resets automatically when dayCount changes.
//
// Skill casts are already tracked authoritatively by SkillManager via the API;
// this class covers free-fight uses, item daily limits, and adventure-based
// resources that the skills API doesn't expose.
class DailyResourceTracker {

    data class State(
        val dayCount: Int = -1,
        val itemUses: Map<String, Int> = emptyMap(),       // item name (lower) → times used
        val freeFightsUsed: Map<String, Int> = emptyMap(), // source name → count
        val freePullsUsed: Int = 0,
        val stonestoneFightsDone: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // Called whenever CharacterState is updated so we can detect day rollover.
    fun syncDay(newDayCount: Int) {
        if (newDayCount != _state.value.dayCount && newDayCount >= 0) {
            _state.value = State(dayCount = newDayCount)
        }
    }

    fun recordItemUse(itemName: String, times: Int = 1) {
        val key = itemName.lowercase()
        val updated = _state.value.itemUses.toMutableMap()
        updated[key] = (updated[key] ?: 0) + times
        _state.value = _state.value.copy(itemUses = updated)
    }

    fun recordFreeFight(source: String, times: Int = 1) {
        val updated = _state.value.freeFightsUsed.toMutableMap()
        updated[source] = (updated[source] ?: 0) + times
        _state.value = _state.value.copy(freeFightsUsed = updated)
    }

    fun recordFreePull() {
        _state.value = _state.value.copy(freePullsUsed = _state.value.freePullsUsed + 1)
    }

    fun timesItemUsedToday(itemName: String): Int =
        _state.value.itemUses[itemName.lowercase()] ?: 0

    fun freeFightsUsedFor(source: String): Int =
        _state.value.freeFightsUsed[source] ?: 0

    fun canUseItem(itemName: String, dailyLimit: Int): Boolean {
        if (dailyLimit <= 0) return true
        return timesItemUsedToday(itemName) < dailyLimit
    }

    // Resets the tracker for a new day (also called automatically via syncDay).
    fun reset(dayCount: Int = _state.value.dayCount) {
        _state.value = State(dayCount = dayCount)
    }
}
