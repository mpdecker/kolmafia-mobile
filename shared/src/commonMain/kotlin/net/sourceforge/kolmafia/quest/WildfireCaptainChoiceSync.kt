package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.WildfireCampManager

/**
 * Desktop [ChoiceControl] Fire Captain Hagnk choice 1451.
 */
object WildfireCaptainChoiceSync {

    const val CHOICE_ID = 1451

    const val CHARGE_PREF = "_fireExtinguisherCharge"
    const val REFILLED_PREF = "_fireExtinguisherRefilled"

    private val ZID_PATTERN = Regex("""zid=([^&]*)""", RegexOption.IGNORE_CASE)

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = false
        when (decision) {
            1 -> {
                val zid = ZID_PATTERN.find(choiceUrl)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                if (zid.isNotEmpty()) {
                    WildfireCampManager.reduceFireLevel(preferences, zid)
                    changed = true
                }
            }
            3 -> {
                if (html.contains("Hagnk takes your fire extinguisher")) {
                    preferences.setInt(CHARGE_PREF, 100)
                    preferences.setBoolean(REFILLED_PREF, true)
                    changed = true
                }
            }
        }
        WildfireCampManager.parseCaptainHtml(preferences, html)
        return changed || html.contains("<option", ignoreCase = true)
    }
}
