package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop Unpermery choice 812 skill removal and karma refund. */
object UnpermeryChoiceSync {
    const val CHOICE_ID = 812
    private val pattern = Regex("""Turning (.+?)(?: \(HP\)) into (\d+) karma\.""")

    fun apply(choiceId: Int, decision: Int, html: String, preferences: Preferences?, removeSkill: (String) -> Unit): Boolean {
        if (choiceId != CHOICE_ID || decision != 1 || preferences == null) return false
        val match = pattern.find(html) ?: return false
        removeSkill(match.groupValues[1])
        preferences.setInt("bankedKarma", preferences.getInt("bankedKarma", 0) + match.groupValues[2].toInt())
        return true
    }
}
