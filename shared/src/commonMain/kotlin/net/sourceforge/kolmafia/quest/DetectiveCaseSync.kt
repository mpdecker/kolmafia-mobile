package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager] wham.php detective-case increment.
 */
object DetectiveCaseSync {

    const val SOLVED = "Congratulations! You solved the case"

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
}
