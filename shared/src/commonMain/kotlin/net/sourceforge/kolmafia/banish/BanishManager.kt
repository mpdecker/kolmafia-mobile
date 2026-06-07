// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishManager.kt
package net.sourceforge.kolmafia.banish

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Tracks banished monsters across adventure turns.
 *
 * Serialization format for [Preferences.BANISHED_MONSTERS]:
 *   Records separated by `|`; each record is `monsterName:banisherName:turnBanished`
 *   using `split(":", limit = 3)` so monster names containing `:` are safe.
 *
 * Usage:
 *   - Call [load] once after login.
 *   - Call [banishMonster] when a combat banish is detected.
 *   - Call [isBanished] before adventuring to skip banished monsters.
 *   - Call [clearExpiredAndRollover] at login to remove stale rollover banishes.
 */
class BanishManager(private val preferences: Preferences) {
    private val _state = MutableStateFlow(BanishState())
    val state: StateFlow<BanishState> = _state.asStateFlow()

    /**
     * Records a banish, replacing any existing entry for the same monster.
     * Persists to preferences immediately.
     */
    fun banishMonster(monsterName: String, banisher: Banisher, currentTurn: Int) {
        val existing = _state.value.monsters.toMutableList()
        existing.removeIf { it.monsterName.equals(monsterName, ignoreCase = true) }
        existing.add(BanishedMonster(monsterName, banisher, currentTurn))
        _state.value = BanishState(existing)
        save()
    }

    /** Returns true if [monsterName] has an active (non-expired) banish at [currentTurn]. */
    fun isBanished(monsterName: String, currentTurn: Int): Boolean =
        _state.value.monsters.any { b ->
            b.monsterName.equals(monsterName, ignoreCase = true) && !b.isExpired(currentTurn)
        }

    /**
     * Returns a map of monster name to [Banisher] for all currently active banishes.
     * Used by the ASH `banishers_used()` function.
     */
    fun getActiveBanishes(currentTurn: Int): Map<String, Banisher> =
        _state.value.monsters
            .filter { !it.isExpired(currentTurn) }
            .associate { it.monsterName to it.banisher }

    /**
     * Removes all [ResetType.ROLLOVER], [ResetType.AVATAR], and [ResetType.TURN_ROLLOVER] banishes
     * (they reset every rollover), and also removes expired [ResetType.TURNS] banishes.
     * [ResetType.NEVER] banishes (Ice House) are kept.
     * Call this at login after loading state.
     */
    fun clearExpiredAndRollover(currentTurn: Int) {
        _state.value = _state.value.copy(
            monsters = _state.value.monsters.filter { b ->
                when (b.banisher.resetType) {
                    ResetType.ROLLOVER, ResetType.AVATAR, ResetType.TURN_ROLLOVER -> false
                    ResetType.TURNS -> !b.isExpired(currentTurn)
                    ResetType.NEVER -> true
                }
            }
        )
        save()
    }

    /** Serializes [state] to [Preferences.BANISHED_MONSTERS]. */
    fun save() {
        val serialized = _state.value.monsters.joinToString("|") { b ->
            "${b.monsterName}:${b.banisher.canonicalName}:${b.turnBanished}"
        }
        preferences.setString(Preferences.BANISHED_MONSTERS, serialized)
    }

    /** Restores [state] from [Preferences.BANISHED_MONSTERS]. Call once after login. */
    fun load() {
        val raw = preferences.getString(Preferences.BANISHED_MONSTERS)
        if (raw.isBlank()) { _state.value = BanishState(); return }
        val monsters = raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":", limit = 3)
            if (parts.size < 3) return@mapNotNull null
            val banisher = Banisher.fromName(parts[1])
            val turn = parts[2].toIntOrNull() ?: return@mapNotNull null
            BanishedMonster(monsterName = parts[0], banisher = banisher, turnBanished = turn)
        }
        _state.value = BanishState(monsters)
    }
}
