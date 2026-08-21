package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.MayamAvailability
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl.parseMayamCalendar] for choice 1527.
 */
object MayamChoiceSync {

    const val CHOICE_ID = 1527

    private val MAYAM_SYMBOLS = Regex(
        """<img data-pos="\d"[^>]*?class="(used)?"\s*alt="([^\s]+)\s""",
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        parseMayamCalendar(html, preferences)
        return true
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        parseMayamCalendar(html, preferences)
        return true
    }

    /** Desktop [ChoiceControl.parseMayamCalendar]. */
    fun parseMayamCalendar(html: String, preferences: Preferences) {
        var yams = 0
        val used = mutableListOf<String>()
        MAYAM_SYMBOLS.findAll(html).forEach { match ->
            val shouldAdd = match.groupValues[1] == "used"
            var symbol = match.groupValues[2].lowercase()
            if (symbol == "yam") {
                yams++
                symbol = "yam$yams"
            }
            if (shouldAdd) used += symbol
        }
        preferences.setString(MayamAvailability.SYMBOLS_USED_PREF, used.joinToString(","))
    }
}
