package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Mushroom District Costume/Badge shops 1407 / 1408.
 */
object PlumberShopChoiceSync {

    const val COSTUME_CHOICE = 1407
    const val BADGE_CHOICE = 1408

    const val COIN = 10454
    const val COSTUME_COST_PREF = "plumberCostumeCost"
    const val COSTUME_WORN_PREF = "plumberCostumeWorn"
    const val BADGE_COST_PREF = "plumberBadgeCost"

    private val COSTUME_PATTERN = Regex(
        """<form.*?name=option value=(\d).*?type=submit value="(.*?) Costume".*?>(\d+) coins<.*?</form>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val BADGE_PATTERN = Regex("""Current cost: (\d+) coins\.""", RegexOption.IGNORE_CASE)

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            COSTUME_CHOICE -> {
                var cost = 0
                var carpenter = false
                var gardener = false
                var ballerina = false
                COSTUME_PATTERN.findAll(html).forEach { match ->
                    cost = match.groupValues[3].toIntOrNull() ?: cost
                    when (match.groupValues[2]) {
                        "Carpenter" -> carpenter = true
                        "Gardener" -> gardener = true
                        "Ballerina" -> ballerina = true
                    }
                }
                val wearing = when {
                    !carpenter -> "muscle"
                    !gardener -> "mysticality"
                    !ballerina -> "moxie"
                    else -> "none"
                }
                preferences.setInt(COSTUME_COST_PREF, cost)
                preferences.setString(COSTUME_WORN_PREF, wearing)
                true
            }
            BADGE_CHOICE -> {
                val cost = BADGE_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                preferences.setInt(BADGE_COST_PREF, cost)
                true
            }
            else -> false
        }
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            COSTUME_CHOICE -> {
                val costume = when {
                    html.contains("You slip into something a little more carpentable") -> "muscle"
                    html.contains("You let down your guard and put on the gardener costume") -> "mysticality"
                    html.contains("Todge holds out a tutu and you jump into it") -> "moxie"
                    else -> return false
                }
                val cost = preferences.getInt(COSTUME_COST_PREF, 0)
                if (cost > 0) consumeItem(COIN, cost)
                preferences.setInt(COSTUME_COST_PREF, cost + 50)
                preferences.setString(COSTUME_WORN_PREF, costume)
                true
            }
            BADGE_CHOICE -> {
                if (!html.contains("You acquire a skill")) return false
                val cost = preferences.getInt(BADGE_COST_PREF, 0)
                if (cost > 0) consumeItem(COIN, cost)
                preferences.setInt(BADGE_COST_PREF, cost + 25)
                true
            }
            else -> false
        }
    }
}
