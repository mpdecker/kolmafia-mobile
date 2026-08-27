// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishManager.kt
package net.sourceforge.kolmafia.banish

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Tracks banished monsters and phyla across adventure turns.
 *
 * Pref format matches desktop colon triples (`name:banisher:turn:...`). Mobile also
 * dual-reads legacy `|`-separated records written by earlier builds.
 *
 * Phases 1071–1090: per-banisher [Banisher.queueSize] FIFO eviction + phylum banishes.
 */
class BanishManager(
    private val preferences: Preferences,
) {
    private val _state = MutableStateFlow(BanishState())
    val state: StateFlow<BanishState> = _state.asStateFlow()

    /**
     * Records a banish with desktop queue-size eviction.
     * For [BanishType.PHYLUM] banishes, [monsterName] is resolved to phylum via [MonsterDatabase]
     * unless [phylumOverride] is supplied.
     */
    fun banishMonster(
        monsterName: String,
        banisher: Banisher,
        currentTurn: Int,
        adventureResult: Boolean = true,
        phylumOverride: String? = null,
    ) {
        val entry = when (banisher.banishType) {
            BanishType.PHYLUM -> {
                phylumOverride
                    ?: resolvePhylum(monsterName)
                    ?: return
            }
            BanishType.MONSTER -> monsterName
        }

        val queueSize = banisher.queueSize
        if (countBanishes(banisher) >= queueSize) {
            if (queueSize == 1 &&
                entry.equals(firstBanished(banisher), ignoreCase = true) &&
                !banisher.resetType.isTurnReset
            ) {
                return
            }
            removeOldestBanish(banisher)
        }

        when (banisher) {
            Banisher.LEFT_ZOOT_KICK -> removeOldestBanish(Banisher.RIGHT_ZOOT_KICK)
            Banisher.RIGHT_ZOOT_KICK -> removeOldestBanish(Banisher.LEFT_ZOOT_KICK)
            else -> Unit
        }

        val turnBanished = when {
            banisher.resetType == ResetType.NEVER -> 0
            !adventureResult || banisher.isTurnFree -> currentTurn
            else -> currentTurn + 1
        }

        val record = BanishedMonster(entry, banisher, turnBanished)
        val s = _state.value
        _state.value = when (banisher.banishType) {
            BanishType.PHYLUM -> s.copy(phyla = s.phyla + record)
            BanishType.MONSTER -> s.copy(monsters = s.monsters + record)
        }
        writeLegacyPrefs(entry, banisher)
        save()
    }

    fun formatStatus(currentTurn: Int): String {
        val active = allEntries().filter { !it.isExpired(currentTurn) }
        if (active.isEmpty()) return "No current banishes"
        return buildString {
            appendLine("Monsters Banished\tBanished By\tOn Turn\tTurns Left")
            for (b in active) {
                val left = when (b.banisher.resetType) {
                    ResetType.TURNS, ResetType.TURN_ROLLOVER -> {
                        val duration = b.banisher.effectiveDuration()
                        if (duration > 0) {
                            (b.turnBanished + duration - currentTurn).coerceAtLeast(0).toString()
                        } else {
                            "until rollover"
                        }
                    }
                    ResetType.NEVER -> "never"
                    ResetType.EFFECT -> "until effect expires"
                    ResetType.COSMIC_BOWLING_BALL -> "until ball returns"
                    else -> "until rollover"
                }
                appendLine("${b.monsterName}\t${b.banisher.canonicalName}\t${b.turnBanished}\t$left")
            }
        }.trimEnd()
    }

    fun isBanished(monsterName: String, currentTurn: Int): Boolean {
        if (_state.value.monsters.any { b ->
                b.monsterName.equals(monsterName, ignoreCase = true) && !b.isExpired(currentTurn)
            }
        ) {
            return true
        }
        val phylum = resolvePhylum(monsterName) ?: return false
        return isBanishedPhylum(phylum, currentTurn)
    }

    /** Desktop [BanishManager.isBanishedPhylum]. */
    fun isBanishedPhylum(phylum: String, currentTurn: Int): Boolean =
        _state.value.phyla.any { b ->
            b.monsterName.equals(phylum, ignoreCase = true) && !b.isExpired(currentTurn)
        }

    /**
     * Returns a map of monster/phylum name to [Banisher] for all currently active banishes.
     * Used by the ASH `banishers_used()` function.
     */
    fun getActiveBanishes(currentTurn: Int): Map<String, Banisher> =
        allEntries()
            .filter { !it.isExpired(currentTurn) }
            .associate { it.monsterName to it.banisher }

    /** Desktop [BanishManager.banishedBy] — active banishers for a monster (incl. phylum). */
    fun banishedBy(monsterName: String, currentTurn: Int): List<Banisher> {
        val monsterHits = _state.value.monsters
            .filter { !it.isExpired(currentTurn) && it.monsterName.equals(monsterName, ignoreCase = true) }
            .map { it.banisher }
        val phylum = resolvePhylum(monsterName) ?: return monsterHits
        val phylumHits = _state.value.phyla
            .filter { !it.isExpired(currentTurn) && it.monsterName.equals(phylum, ignoreCase = true) }
            .map { it.banisher }
        return monsterHits + phylumHits
    }

    /** Desktop [BanishManager.resetRollover] — rollover-only (not TURN_ROLLOVER) banishes. */
    fun resetRollover(): Int {
        val before = allEntries().size
        _state.value = BanishState(
            monsters = _state.value.monsters.filter {
                it.banisher.resetType != ResetType.ROLLOVER &&
                    it.banisher.resetType != ResetType.COSMIC_BOWLING_BALL
            },
            phyla = _state.value.phyla.filter {
                it.banisher.resetType != ResetType.ROLLOVER &&
                    it.banisher.resetType != ResetType.COSMIC_BOWLING_BALL
            },
        )
        val cleared = before - allEntries().size
        if (cleared > 0) save()
        return cleared
    }

    /** Desktop [BanishManager.resetCosmicBowlingBall]. */
    fun resetCosmicBowlingBall(): Int {
        val before = allEntries().size
        _state.value = BanishState(
            monsters = _state.value.monsters.filter {
                it.banisher.resetType != ResetType.COSMIC_BOWLING_BALL
            },
            phyla = _state.value.phyla.filter {
                it.banisher.resetType != ResetType.COSMIC_BOWLING_BALL
            },
        )
        val cleared = before - allEntries().size
        if (cleared > 0) save()
        return cleared
    }

    /** Desktop effect-reset path (Roar like a Lion). */
    fun resetEffectBanishes(): Int = removeByResetType(ResetType.EFFECT)

    /**
     * Removes all [ResetType.ROLLOVER], [ResetType.AVATAR], [ResetType.TURN_ROLLOVER],
     * [ResetType.EFFECT], and [ResetType.COSMIC_BOWLING_BALL] banishes, and expired
     * [ResetType.TURNS] banishes. [ResetType.NEVER] banishes (Ice House) are kept.
     */
    fun clearExpiredAndRollover(currentTurn: Int) {
        fun keep(b: BanishedMonster): Boolean = when (b.banisher.resetType) {
            ResetType.ROLLOVER, ResetType.AVATAR, ResetType.TURN_ROLLOVER,
            ResetType.EFFECT, ResetType.COSMIC_BOWLING_BALL,
            -> false
            ResetType.TURNS -> !b.isExpired(currentTurn)
            ResetType.NEVER -> true
        }
        _state.value = BanishState(
            monsters = _state.value.monsters.filter(::keep),
            phyla = _state.value.phyla.filter(::keep),
        )
        save()
    }

    /** Desktop ascension clears [ResetType.AVATAR] banishes (Prism Break). */
    fun clearAvatarBanishes() {
        _state.value = BanishState(
            monsters = _state.value.monsters.filter { it.banisher.resetType != ResetType.AVATAR },
            phyla = _state.value.phyla.filter { it.banisher.resetType != ResetType.AVATAR },
        )
        save()
    }

    /**
     * Desktop [BanishManager.removeBanishByBanisher] —
     * removes every active banish recorded with [banisher] (e.g. freeing Ice House).
     */
    fun removeBanishByBanisher(banisher: Banisher): Int {
        val before = allEntries().size
        _state.value = BanishState(
            monsters = _state.value.monsters.filter { it.banisher != banisher },
            phyla = _state.value.phyla.filter { it.banisher != banisher },
        )
        val cleared = before - allEntries().size
        if (cleared > 0) save()
        return cleared
    }

    fun countBanishes(banisher: Banisher): Int =
        allEntries().count { it.banisher == banisher }

    fun save() {
        preferences.setString(
            Preferences.BANISHED_MONSTERS,
            serializeColon(_state.value.monsters),
        )
        preferences.setString(
            Preferences.BANISHED_PHYLA,
            serializeColon(_state.value.phyla),
        )
    }

    fun load() {
        _state.value = BanishState(
            monsters = parsePref(preferences.getString(Preferences.BANISHED_MONSTERS)),
            phyla = parsePref(preferences.getString(Preferences.BANISHED_PHYLA)),
        )
    }

    private fun allEntries(): List<BanishedMonster> =
        _state.value.monsters + _state.value.phyla

    private fun firstBanished(banisher: Banisher): String? =
        allEntries().firstOrNull { it.banisher == banisher }?.monsterName

    private fun removeOldestBanish(banisher: Banisher) {
        val oldest = allEntries()
            .filter { it.banisher == banisher }
            .minByOrNull { it.turnBanished }
            ?: return
        _state.value = BanishState(
            monsters = _state.value.monsters.filter { it != oldest },
            phyla = _state.value.phyla.filter { it != oldest },
        )
    }

    private fun removeByResetType(type: ResetType): Int {
        val before = allEntries().size
        _state.value = BanishState(
            monsters = _state.value.monsters.filter { it.banisher.resetType != type },
            phyla = _state.value.phyla.filter { it.banisher.resetType != type },
        )
        val cleared = before - allEntries().size
        if (cleared > 0) save()
        return cleared
    }

    private fun resolvePhylum(monsterName: String): String? =
        MonsterDatabase.getByName(monsterName)?.phylum?.takeIf { it.isNotBlank() }

    private fun writeLegacyPrefs(entry: String, banisher: Banisher) {
        when (banisher) {
            Banisher.NANORHINO ->
                preferences.setString("_nanorhinoBanishedMonster", entry)
            Banisher.BANISHING_SHOUT, Banisher.HOWL_OF_THE_ALPHA -> {
                val prior = preferences.getString("banishingShoutMonsters")
                    .split("|")
                    .filter { it.isNotBlank() }
                    .take(2)
                preferences.setString(
                    "banishingShoutMonsters",
                    (listOf(entry) + prior).joinToString("|"),
                )
            }
            Banisher.STAFF_OF_THE_STANDALONE_CHEESE -> {
                val prior = preferences.getString("_jiggleCheesedMonsters")
                    .split("|")
                    .filter { it.isNotBlank() }
                preferences.setString(
                    "_jiggleCheesedMonsters",
                    (listOf(entry) + prior).joinToString("|"),
                )
            }
            else -> Unit
        }
    }

    companion object {
        internal fun serializeColon(entries: List<BanishedMonster>): String =
            entries.joinToString(":") { b ->
                "${b.monsterName}:${b.banisher.canonicalName}:${b.turnBanished}"
            }

        /**
         * Dual-read: desktop colon triples, or legacy `|`-separated `name:banisher:turn` records.
         */
        internal fun parsePref(raw: String): List<BanishedMonster> {
            if (raw.isBlank()) return emptyList()
            return if (raw.contains('|')) {
                raw.split("|").mapNotNull { parseTriple(it) }
            } else {
                val tokens = raw.split(':')
                val out = mutableListOf<BanishedMonster>()
                var i = 0
                while (i + 2 < tokens.size) {
                    val turn = tokens[i + 2].toIntOrNull() ?: break
                    out += BanishedMonster(
                        monsterName = tokens[i],
                        banisher = Banisher.fromName(tokens[i + 1]),
                        turnBanished = turn,
                    )
                    i += 3
                }
                out
            }
        }

        private fun parseTriple(entry: String): BanishedMonster? {
            val parts = entry.split(":", limit = 3)
            if (parts.size < 3) return null
            val turn = parts[2].toIntOrNull() ?: return null
            return BanishedMonster(parts[0], Banisher.fromName(parts[1]), turn)
        }
    }
}
