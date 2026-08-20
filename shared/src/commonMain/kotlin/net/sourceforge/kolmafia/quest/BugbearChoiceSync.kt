package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BugbearManager

/**
 * Desktop [ChoiceControl] Machines! choice 588 sonar clear.
 */
object BugbearChoiceSync {

    const val CHOICE_ID = 588
    const val SONAR_ZONE = "Sonar"

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains("The batbugbears around you start acting weird")) return false
        BugbearManager.clearShipZone(SONAR_ZONE, preferences)
        return true
    }
}
