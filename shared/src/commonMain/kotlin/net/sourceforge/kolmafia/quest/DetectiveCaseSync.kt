package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager] wham.php detective-case increment + Precinct choice 1193 visit.
 */
object DetectiveCaseSync {

    const val CHOICE_ID = 1193
    const val SOLVED = "Congratulations! You solved the case"

    private val CASE_PATTERN = Regex("""\((\d+) more case""")

    fun applyFromVisit(
        url: String?,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (url != null && !url.contains("wham.php", ignoreCase = true)) return false
        if (!html.contains(SOLVED)) return false
        val current = preferences.getInt("_detectiveCasesCompleted", 0)
        preferences.setInt("_detectiveCasesCompleted", current + 1)
        return true
    }

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val remaining = CASE_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        preferences.setInt("_detectiveCasesCompleted", 3 - remaining)
        return true
    }
}
