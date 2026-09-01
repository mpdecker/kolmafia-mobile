package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop ConditionsCommand pseudo-item counters (pirate insult / flyer ML / chasm bridge). */
object GoalPseudoConditions {

    const val VALID_PIRATE_INSULTS = 8
    const val MAX_CHASM_PROGRESS = 30

    enum class Kind { PIRATE_INSULT, ARENA_FLYER_ML, CHASM_BRIDGE }

    fun countPirateInsults(preferences: Preferences): Int {
        var count = 0
        for (i in 1..VALID_PIRATE_INSULTS) {
            if (preferences.getBoolean("lastPirateInsult$i", false)) count++
        }
        return count
    }

    fun arenaFlyerMl(preferences: Preferences): Int =
        preferences.getInt("flyeredML", 0)

    fun chasmProgress(preferences: Preferences): Int =
        preferences.getInt("chasmBridgeProgress", 0)

    fun currentCount(kind: Kind, preferences: Preferences): Int = when (kind) {
        Kind.PIRATE_INSULT -> countPirateInsults(preferences)
        Kind.ARENA_FLYER_ML -> arenaFlyerMl(preferences)
        Kind.CHASM_BRIDGE -> chasmProgress(preferences)
    }

    fun isMet(kind: Kind, target: Int, preferences: Preferences): Boolean =
        currentCount(kind, preferences) >= target
}
