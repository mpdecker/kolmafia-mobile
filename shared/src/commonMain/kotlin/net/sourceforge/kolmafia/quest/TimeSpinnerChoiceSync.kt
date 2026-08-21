package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Time-Spinner choices 1195–1196, 1198–1199.
 */
object TimeSpinnerChoiceSync {

    const val SPINNING = 1195
    const val RECENT_FIGHT = 1196
    const val TIME_PRANK = 1198
    const val FAR_FUTURE = 1199
    const val MINUTES_PREF = "_timeSpinnerMinutesUsed"

    val CHOICE_IDS = setOf(SPINNING, RECENT_FIGHT, TIME_PRANK, FAR_FUTURE)

    private val MEDALS_PATTERN = Regex("""memory of earning <b>(\d+) medal""")

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        choiceUrl: String = "",
        html: String = "",
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            SPINNING, RECENT_FIGHT -> applyMinutes(choiceId, decision, preferences, choiceUrl)
            TIME_PRANK -> {
                if (decision != 1) return false
                if (!html.contains("paradoxical time copy")) return false
                preferences.setInt(MINUTES_PREF, preferences.getInt(MINUTES_PREF, 0) + 1)
                true
            }
            FAR_FUTURE -> {
                when {
                    html.contains("item appears in the replicator") ||
                        html.contains("convoluted nature of time-travel") -> {
                        preferences.setBoolean("_timeSpinnerReplicatorUsed", true)
                        true
                    }
                    else -> {
                        val medals = MEDALS_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
                            ?: return false
                        preferences.setInt("timeSpinnerMedals", medals)
                        true
                    }
                }
            }
            else -> false
        }
    }

    private fun applyMinutes(
        choiceId: Int,
        decision: Int,
        preferences: Preferences,
        choiceUrl: String,
    ): Boolean {
        val delta = when (choiceId) {
            SPINNING -> when (decision) {
                3 -> 1
                4 -> 2
                else -> return false
            }
            RECENT_FIGHT -> {
                if (decision != 1) return false
                if (choiceUrl.contains("monid=0")) return false
                3
            }
            else -> return false
        }
        preferences.setInt(MINUTES_PREF, preferences.getInt(MINUTES_PREF, 0) + delta)
        return true
    }
}
