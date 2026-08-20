package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Time is a Möbius Strip choice 1562.
 */
object MobiusChoiceSync {

    const val CHOICE_ID = 1562

    fun applyVisit(
        choiceId: Int,
        preferences: Preferences?,
        turnsPlayed: Int,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setInt("_lastMobiusStripTurn", turnsPlayed)
        val encounters = preferences.getInt("_mobiusStripEncounters", 0)
        preferences.setInt("_mobiusStripEncounters", encounters + 1)
        return true
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        turnsPlayed: Int,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        return when {
            html.contains("stock certificate") -> {
                preferences.setInt("stockCertificateTurn", turnsPlayed)
                val prior = preferences.getString("stockCertificateTurns", "")
                preferences.setString(
                    "stockCertificateTurns",
                    if (prior.isEmpty()) turnsPlayed.toString() else "$prior,$turnsPlayed",
                )
                true
            }
            html.contains("In an effort to repair the timeline") -> {
                val current = preferences.getInt("tryToRememberCharges", 0)
                preferences.setInt("tryToRememberCharges", current + 3)
                true
            }
            else -> false
        }
    }
}
