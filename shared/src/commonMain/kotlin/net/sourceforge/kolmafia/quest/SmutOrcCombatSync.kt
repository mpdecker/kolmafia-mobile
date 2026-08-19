package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestData] smut-orc NC progress writers.
 */
object SmutOrcCombatSync {

    const val PREF = "smutOrcNoncombatProgress"
    const val MAX_PROGRESS = 15

    private val MONSTERS = setOf(
        "smut orc jacker",
        "smut orc nailer",
        "smut orc pipelayer",
        "smut orc screwer",
    )

    fun apply(monster: String, html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (monster.trim().lowercase() !in MONSTERS) return false
        val delta = when {
            html.contains("another smut orc shiver") -> 1
            html.contains("you see two smut orcs") -> 2
            html.contains("smut orcs huddling together") -> 3
            html.contains("windows being slammed shut") -> 4
            html.contains("Dozens of nearby smut orcs") -> 5
            else -> return false
        }
        val next = (preferences.getInt(PREF, 0) + delta).coerceAtMost(MAX_PROGRESS)
        preferences.setInt(PREF, next)
        return true
    }
}
