package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] A Cooler Yeti Conversation choice 1560.
 */
object CoolerYetiChoiceSync {

    const val CHOICE_ID = 1560

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (html.contains("Make my next drink impossibly cold")) {
            preferences.setBoolean("_coolerYetiAdventures", false)
        } else {
            preferences.setBoolean("_coolerYetiAdventures", true)
        }
        when {
            !html.contains("He's busy with") -> preferences.setString("coolerYetiMode", "")
            html.contains("He's busy with a cooler") ->
                preferences.setString("coolerYetiMode", "adventures")
            html.contains("He's busy with an ice cup") ->
                preferences.setString("coolerYetiMode", "effect")
            html.contains("He's busy with his bar") ->
                preferences.setString("coolerYetiMode", "bar")
            html.contains("He's busy with a flawless ice cube") ->
                preferences.setString("coolerYetiMode", "stats")
        }
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        return when (decision) {
            2 -> {
                preferences.setBoolean("_coolerYetiAdventures", true)
                preferences.setString("coolerYetiMode", "adventures")
                true
            }
            3 -> {
                preferences.setString("coolerYetiMode", "effect")
                true
            }
            4 -> {
                preferences.setString("coolerYetiMode", "bar")
                true
            }
            5 -> {
                preferences.setString("coolerYetiMode", "stats")
                true
            }
            else -> false
        }
    }
}
