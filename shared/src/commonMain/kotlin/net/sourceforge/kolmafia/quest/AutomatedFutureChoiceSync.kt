package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Automated Future choices 1512/1513 —
 * `_automatedFutureSide` + `_automatedFutureManufactures`.
 */
object AutomatedFutureChoiceSync {

    const val SOLENOIDS = 1512
    const val BEARINGS = 1513

    val CHOICE_IDS = setOf(SOLENOIDS, BEARINGS)

    const val SIDE_PREF = "_automatedFutureSide"
    const val MANUFACTURES_PREF = "_automatedFutureManufactures"
    const val MAX_MANUFACTURES = 11

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId !in CHOICE_IDS || preferences == null) return false
        return when {
            html.contains("don't even think about pressing that button") -> {
                // Opposite side is locked; current choice is the other side.
                preferences.setString(
                    SIDE_PREF,
                    if (choiceId == SOLENOIDS) "bearings" else "solenoids",
                )
                true
            }
            html.contains("You've already pushed the button eleven times today") -> {
                preferences.setString(
                    SIDE_PREF,
                    if (choiceId == SOLENOIDS) "solenoids" else "bearings",
                )
                preferences.setInt(MANUFACTURES_PREF, MAX_MANUFACTURES)
                true
            }
            else -> false
        }
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId !in CHOICE_IDS || preferences == null) return false
        if (decision != 1 || !html.contains("You press the button.")) return false
        preferences.setString(
            SIDE_PREF,
            if (choiceId == SOLENOIDS) "solenoids" else "bearings",
        )
        val current = preferences.getInt(MANUFACTURES_PREF, 0)
        preferences.setInt(MANUFACTURES_PREF, (current + 1).coerceAtMost(MAX_MANUFACTURES))
        return true
    }
}
