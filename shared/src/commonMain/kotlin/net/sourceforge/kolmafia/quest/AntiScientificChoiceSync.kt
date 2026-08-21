package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] The Antiscientific Method choice 1522.
 */
object AntiScientificChoiceSync {

    const val CHOICE_ID = 1522

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        lastVisitedLocationName: String = "",
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains("smashed scientific equipment")) return false
        val name = lastVisitedLocationName.trim()
        if (name.isEmpty()) return false
        val existing = preferences.getString("antiScientificMethod", "").trim()
        preferences.setString(
            "antiScientificMethod",
            if (existing.isEmpty()) name else "$existing|$name",
        )
        return true
    }
}
