package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Spacegate Vaccination Machine choice 1234.
 */
object SpacegateVaccinatorChoiceSync {

    const val CHOICE_ID = 1234
    const val VACCINE_USED_PREF = "_spacegateVaccine"

    private val VACCINE_PATTERN =
        Regex("""option value=(\d+).*?class=button type=submit value="([^"]*)""")

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = false
        for (match in VACCINE_PATTERN.findAll(html)) {
            val setting = "spacegateVaccine${match.groupValues[1]}"
            val button = match.groupValues[2]
            when {
                button.startsWith("Select Vaccine") -> {
                    preferences.setBoolean(setting, true)
                    changed = true
                }
                button.startsWith("Unlock Vaccine") -> {
                    preferences.setBoolean(setting, false)
                    changed = true
                }
            }
        }
        return changed
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = false
        if (html.contains("New vaccine unlocked!")) {
            preferences.setBoolean("spacegateVaccine$decision", true)
            changed = true
        } else if (html.contains("You acquire an effect")) {
            preferences.setBoolean(VACCINE_USED_PREF, true)
            changed = true
        }
        return changed
    }
}
