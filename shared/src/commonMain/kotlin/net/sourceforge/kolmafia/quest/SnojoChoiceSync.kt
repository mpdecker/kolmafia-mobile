package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] X-32-F Combat Training Snowman Control Console choice 1118.
 */
object SnojoChoiceSync {

    const val CHOICE_ID = 1118
    private val CONSOLE_PATTERN = Regex("<b>(.*?) MODE</b>")

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val setting = when (decision) {
            1 -> "MUSCLE"
            2 -> "MYSTICALITY"
            3 -> "MOXIE"
            4 -> "TOURNAMENT"
            else -> return false
        }
        preferences.setString("snojoSetting", setting)
        return true
    }

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val setting = CONSOLE_PATTERN.find(html)?.groupValues?.get(1)?.trim().orEmpty()
        preferences.setString("snojoSetting", setting)
        return true
    }
}
