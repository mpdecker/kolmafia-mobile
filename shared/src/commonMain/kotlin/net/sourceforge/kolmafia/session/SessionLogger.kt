package net.sourceforge.kolmafia.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

/** Persists recent [GameEvent] records for session_logs() ASH and debugging. */
class SessionLogger(
    private val preferences: Preferences,
    private val eventBus: GameEventBus,
) {
    companion object {
        const val SESSION_LOG_KEY = "sessionLogLines"
        const val MAX_LINES = 200
    }

    fun start(scope: CoroutineScope) {
        eventBus.events.onEach { append(it) }.launchIn(scope)
    }

    fun append(event: GameEvent) {
        val line = "${formatTimestamp()} ${event::class.simpleName}: ${eventSummary(event)}"
        val existing = preferences.getString(SESSION_LOG_KEY, "")
            .lines()
            .filter { it.isNotBlank() }
            .toMutableList()
        existing.add(line)
        while (existing.size > MAX_LINES) existing.removeAt(0)
        preferences.setString(SESSION_LOG_KEY, existing.joinToString("\n"))
    }

    fun recentLines(days: Int = 1): List<String> {
        val cutoff = days.coerceAtLeast(1)
        return preferences.getString(SESSION_LOG_KEY, "")
            .lines()
            .filter { it.isNotBlank() }
            .takeLast(cutoff * MAX_LINES)
    }

    private fun formatTimestamp(): String =
        net.sourceforge.kolmafia.ash.currentDateTimeString()

    private fun eventSummary(event: GameEvent): String = when (event) {
        is GameEvent.TurnConsumed -> "turn @ ${event.location.name}"
        is GameEvent.CombatFinished -> "combat ${event.monster} won=${event.won}"
        is GameEvent.MonsterBanished -> "banish ${event.monsterName}"
        is GameEvent.ChoiceResolved -> "choice ${event.choiceId} opt=${event.option}"
        is GameEvent.AdventureLoopStopped -> "stopped ${event.reason}"
        is GameEvent.ItemObtained -> "got ${event.item.name}"
        is GameEvent.ItemConsumed -> "used item ${event.itemId}"
        is GameEvent.ItemEquipped -> "equip ${event.item.name}"
        is GameEvent.ItemDiscarded -> "discard ${event.itemId}"
        is GameEvent.ItemCrafted -> "craft ${event.resultItem.name}"
        is GameEvent.MallPurchase -> "mall buy ${event.item.name}"
        is GameEvent.FamiliarSwitched -> "familiar ${event.familiar.race}"
        is GameEvent.FamiliarEquipped -> "fam equip ${event.familiar.race}"
        is GameEvent.FamiliarHatched -> "hatch ${event.familiar.race}"
        is GameEvent.SkillCast -> "cast ${event.skillName}"
        is GameEvent.EffectsRefreshed -> "effects refreshed"
        is GameEvent.ScriptStarted -> "script start ${event.scriptName}"
        is GameEvent.ScriptOutput -> event.line
        is GameEvent.ScriptFinished -> "script done ${event.scriptName} ok=${event.success}"
    }
}
