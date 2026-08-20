package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Dig at Zone choice 1596 —
 * visit parse of remaining digs + postChoice increment (decision != 4).
 */
object ArchSpadeChoiceSync {

    const val CHOICE_ID = 1596

    const val DIGS_PREF = "_archSpadeDigs"
    const val MAX_DIGS = 11

    private val REMAINING_PATTERN =
        Regex("""wherewithal to dig <b>(\d+)</b> more""", RegexOption.IGNORE_CASE)

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val remaining = REMAINING_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        preferences.setInt(DIGS_PREF, (MAX_DIGS - remaining).coerceIn(0, MAX_DIGS))
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision == 4) return false
        val current = preferences.getInt(DIGS_PREF, 0)
        preferences.setInt(DIGS_PREF, (current + 1).coerceAtMost(MAX_DIGS))
        return true
    }
}
