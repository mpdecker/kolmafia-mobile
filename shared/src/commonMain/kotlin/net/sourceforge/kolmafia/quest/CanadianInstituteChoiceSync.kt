package net.sourceforge.kolmafia.quest

/**
 * Desktop ClanRumpusRequest Canadian Institute choice 770 —
 * Mysticality gym workout completion recognition.
 */
object CanadianInstituteChoiceSync {

    const val CHOICE_ID = 770

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        sessionLog: (String) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || decision != 1) return false
        if (!html.contains("learn from the sages", ignoreCase = true) &&
            !html.contains("feel the burn", ignoreCase = true)
        ) {
            return false
        }
        sessionLog("Workout completed.")
        return true
    }
}
