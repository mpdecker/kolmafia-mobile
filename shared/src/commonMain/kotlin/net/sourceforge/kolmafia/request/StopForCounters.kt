package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.TurnCounter

/**
 * Desktop [GenericRequest.stopForCounters] — adventure-adjacent abort before high-traffic HTTP.
 * Does not invent a full GenericRequest superclass; callers pass turnsUsed / adventureId.
 */
object StopForCounters {
    const val DONT_STOP_PREF = "dontStopForCounters"

    data class Result(
        val shouldStop: Boolean,
        val message: String = "",
    )

    /**
     * Process informational counters, then abort on the first non-informational expired counter
     * unless [DONT_STOP_PREF] is true.
     *
     * @param onCounter desktop counterScript hook; return true when the script handled it
     *   (continue scanning). When false/null and dontStop is false, abort.
     */
    fun check(
        preferences: Preferences,
        currentRun: Int,
        turnsUsed: Int,
        adventureId: String,
        onCounter: ((label: String, turnsRemaining: Int) -> Boolean)? = null,
    ): Result {
        if (turnsUsed <= 0) return Result(shouldStop = false)

        // Informational (exempt) counters first
        while (true) {
            val expired = TurnCounter.getExpiredCounter(
                preferences,
                currentRun,
                turnsUsed,
                adventureId,
                informational = true,
            ) ?: break
            onCounter?.invoke(expired.parsedLabel(), TurnCounter.turnsRemaining(expired, currentRun))
        }

        while (true) {
            val expired = TurnCounter.getExpiredCounter(
                preferences,
                currentRun,
                turnsUsed,
                adventureId,
                informational = false,
            ) ?: break

            val remain = TurnCounter.turnsRemaining(expired, currentRun)
            if (remain < 0) continue

            // Discard conflicting peers (desktop drains remaining same-tier counters)
            while (true) {
                val also = TurnCounter.getExpiredCounter(
                    preferences,
                    currentRun,
                    turnsUsed,
                    adventureId,
                    informational = false,
                ) ?: break
                if (TurnCounter.turnsRemaining(also, currentRun) < 0) continue
            }

            if (onCounter?.invoke(expired.parsedLabel(), remain) == true) {
                continue
            }

            val message = if (remain == 0) {
                "${expired.parsedLabel()} counter expired."
            } else {
                val turnWord = if (remain == 1) "turn" else "turns"
                "${expired.parsedLabel()} counter will expire after $remain more $turnWord."
            }

            if (!preferences.getBoolean(DONT_STOP_PREF, false)) {
                return Result(shouldStop = true, message = message)
            }
        }
        return Result(shouldStop = false)
    }
}
