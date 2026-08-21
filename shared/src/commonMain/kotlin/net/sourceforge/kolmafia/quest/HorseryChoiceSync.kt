package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] The Hostler choice 1266 —
 * visit horse names/stats/current + post rent/return.
 */
object HorseryChoiceSync {

    const val CHOICE_ID = 1266

    private val NAME_PATTERN =
        Regex("""<td valign=top class=small><b>([^<]+)</b> the ([^ ]+) Horse<P>""", RegexOption.IGNORE_CASE)
    private val CRAZY_STAT_PATTERN =
        Regex(
            """Gives you\s+([+-]\d+)% Muscle, ([+-]\d+)% Mysticality, and ([+-]\d+)%""",
            RegexOption.IGNORE_CASE,
        )
    private val RENT_PATTERN =
        Regex("""You rent(?:ed)? the (.*?)!""", RegexOption.IGNORE_CASE)

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setBoolean("horseryAvailable", true)
        var changed = false
        NAME_PATTERN.findAll(html).forEach { match ->
            val name = match.groupValues[1]
            val type = match.groupValues[2]
            val setting = when (type.lowercase()) {
                "crazy" -> "_horseryCrazyName"
                "dark" -> "_horseryDarkName"
                "normal" -> "_horseryNormalName"
                "pale" -> "_horseryPaleName"
                else -> null
            }
            if (setting != null) {
                preferences.setString(setting, name)
                changed = true
            }
        }
        CRAZY_STAT_PATTERN.find(html)?.let { match ->
            preferences.setString("_horseryCrazyMus", match.groupValues[1])
            preferences.setString("_horseryCrazyMys", match.groupValues[2])
            preferences.setString("_horseryCrazyMox", match.groupValues[3])
            changed = true
        }
        val horse = when {
            !html.contains("name=option value=1") -> "normal horse"
            !html.contains("name=option value=2") -> "dark horse"
            !html.contains("name=option value=3") -> "crazy horse"
            !html.contains("name=option value=4") -> "pale horse"
            else -> ""
        }
        preferences.setString("_horsery", horse)
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        return when {
            decision in 1..4 -> {
                val horse = RENT_PATTERN.find(html)?.groupValues?.getOrNull(1) ?: return false
                preferences.setString("_horsery", horse)
                val setting = when (horse.lowercase()) {
                    "crazy horse" -> "_horseryCrazyName"
                    "dark horse" -> "_horseryDarkName"
                    "normal horse" -> "_horseryNormalName"
                    "pale horse" -> "_horseryPaleName"
                    else -> null
                }
                if (setting != null) {
                    preferences.setString("_horseryCurrentName", preferences.getString(setting, ""))
                }
                true
            }
            decision == 5 -> {
                preferences.setString("_horsery", "")
                preferences.setString("_horseryCurrentName", "")
                true
            }
            else -> false
        }
    }
}
