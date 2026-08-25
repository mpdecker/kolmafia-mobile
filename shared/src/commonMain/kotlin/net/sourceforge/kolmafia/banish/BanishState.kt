// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishState.kt
package net.sourceforge.kolmafia.banish

/**
 * A single banished entry (monster name or phylum token).
 *
 * @param monsterName The banished monster name or phylum string (case-insensitive lookups).
 * @param banisher Which banisher was used; [Banisher.UNKNOWN] for unrecognised banishers.
 * @param turnBanished The value of [CharacterState.currentRun] at the time of banishment.
 */
data class BanishedMonster(
    val monsterName: String,
    val banisher: Banisher,
    val turnBanished: Int,
) {
    /** Alias for [monsterName] — desktop record field is `banished`. */
    val banished: String get() = monsterName

    /**
     * Returns true if the banish has expired based on turn count.
     * ROLLOVER / AVATAR / NEVER / EFFECT / COSMIC never expire mid-run via turn count —
     * only explicit clears remove them.
     */
    fun isExpired(currentTurn: Int): Boolean = when (banisher.resetType) {
        ResetType.TURNS, ResetType.TURN_ROLLOVER -> {
            val duration = banisher.effectiveDuration()
            duration > 0 && currentTurn >= turnBanished + duration
        }
        ResetType.ROLLOVER, ResetType.AVATAR, ResetType.NEVER,
        ResetType.EFFECT, ResetType.COSMIC_BOWLING_BALL,
        -> false
    }
}

data class BanishState(
    val monsters: List<BanishedMonster> = emptyList(),
    val phyla: List<BanishedMonster> = emptyList(),
)
