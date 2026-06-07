// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishState.kt
package net.sourceforge.kolmafia.banish

/**
 * A single banished monster entry.
 *
 * @param monsterName  The name of the banished monster (case-insensitive for lookups).
 * @param banisher     Which banisher was used; [Banisher.UNKNOWN] for unrecognised banishers.
 * @param turnBanished The value of [CharacterState.currentRun] at the time of banishment.
 */
data class BanishedMonster(
    val monsterName: String,
    val banisher: Banisher,
    val turnBanished: Int,
) {
    /**
     * Returns true if the banish has expired based on turn count.
     * ROLLOVER, AVATAR, and NEVER banishes never expire during a run — only [clearExpiredAndRollover]
     * removes them at login.
     */
    fun isExpired(currentTurn: Int): Boolean = when (banisher.resetType) {
        ResetType.TURNS, ResetType.TURN_ROLLOVER ->
            banisher.turns > 0 && currentTurn >= turnBanished + banisher.turns
        ResetType.ROLLOVER, ResetType.AVATAR, ResetType.NEVER -> false
    }
}

data class BanishState(val monsters: List<BanishedMonster> = emptyList())
