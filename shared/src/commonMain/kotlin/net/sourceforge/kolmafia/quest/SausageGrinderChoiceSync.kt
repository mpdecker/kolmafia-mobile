package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] A Little Pump and Grind choice 1339 visit prefs.
 * Defers post item-consume.
 */
object SausageGrinderChoiceSync {

    const val CHOICE_ID = 1339

    private val SAUSAGE_PATTERN = Regex(
        """grinder needs (.*?) of the (.*?) required units of filling to make a sausage\.  Your grinder reads "(\d+)" units\.""",
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val match = SAUSAGE_PATTERN.find(html) ?: return false
        val required = match.groupValues[2].replace(",", "").toIntOrNull() ?: return false
        preferences.setInt("_sausagesMade", required / 111 - 1)
        preferences.setString("sausageGrinderUnits", match.groupValues[3])
        return true
    }
}
