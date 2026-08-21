package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [BurningLeavesRequest] visit/post for choice 1510.
 */
object BurningLeavesChoiceSync {

    const val CHOICE_ID = 1510
    const val INFLAMMABLE_LEAF_ID = 11341

    private val LEAVES_BURNED = Regex(
        """You've stoked the fire with <b>(\d+)</b> random lea(?:f|ves) today\.""",
    )
    private val LEAVES_FIELD = Regex("""(?:^|[?&])leaves=(\d+)""", RegexOption.IGNORE_CASE)

    private data class Outcome(
        val leaves: Int,
        val dailyPref: String? = null,
        val dailyMax: Int = -1,
    )

    private val OUTCOMES = listOf(
        Outcome(0),
        Outcome(11, "_leafMonstersFought", 5),
        Outcome(37),
        Outcome(42),
        Outcome(43),
        Outcome(44),
        Outcome(50),
        Outcome(66),
        Outcome(69, "_leafLassosCrafted", 3),
        Outcome(74),
        Outcome(99),
        Outcome(111, "_leafMonstersFought", 5),
        Outcome(222, "_leafDayShortenerCrafted", 1),
        Outcome(666, "_leafMonstersFought", 5),
        Outcome(1111),
        Outcome(6666, "_leafcutterAntEggCrafted", 1),
        Outcome(11111, "_leafTattooCrafted", 1),
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setBoolean("_leavesJumped", !html.contains("Jump in the Flames"))
        LEAVES_BURNED.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("_leavesBurned", it)
        }
        return true
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false

        if (html.contains("You jump in the blazing fire absorb some of the flames and jump out")) {
            preferences.setBoolean("_leavesJumped", true)
            return true
        }
        if (html.contains("You can't thrown in none leaves.") ||
            html.contains("You don't have that many leaves!")
        ) {
            return true
        }

        val leaves = LEAVES_FIELD.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val outcome = OUTCOMES.firstOrNull { it.leaves == leaves } ?: Outcome(0)

        if (html.contains("You don't feel like burning that many leaves again today.")) {
            setToMax(outcome, preferences)
            return true
        }

        if (outcome.leaves == 0 || outcome.dailyPref == null) {
            if (leaves > 0) {
                preferences.setInt("_leavesBurned", preferences.getInt("_leavesBurned", 0) + leaves)
            }
        } else {
            incrementOutcome(outcome, preferences)
        }
        if (leaves > 0) consumeItem(INFLAMMABLE_LEAF_ID, leaves)
        return true
    }

    private fun incrementOutcome(outcome: Outcome, preferences: Preferences) {
        val pref = outcome.dailyPref ?: return
        if (outcome.dailyMax < 0) return
        if (outcome.dailyMax == 1) {
            preferences.setBoolean(pref, true)
        } else {
            val next = (preferences.getInt(pref, 0) + 1).coerceAtMost(outcome.dailyMax)
            preferences.setInt(pref, next)
        }
    }

    private fun setToMax(outcome: Outcome, preferences: Preferences) {
        val pref = outcome.dailyPref ?: return
        if (outcome.dailyMax < 0) return
        if (outcome.dailyMax == 1) {
            preferences.setBoolean(pref, true)
        } else {
            preferences.setInt(pref, outcome.dailyMax)
        }
    }
}
