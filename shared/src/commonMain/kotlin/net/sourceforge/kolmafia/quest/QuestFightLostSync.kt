package net.sourceforge.kolmafia.quest

import kotlin.math.max
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestFightLost] extras beyond NEMESIS loss steps.
 */
object QuestFightLostSync {

    private val CYRUS_PATTERN = Regex("""you remember him getting ([^.]*?)\.""")

    fun apply(
        monster: String,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase == null || monster.isBlank()) return false
        return when (monster.trim().lowercase()) {
            "naughty sorceress (3)" -> {
                questDatabase.setProgress(Quest.FINAL, "step12")
                true
            }
            "cyrus the virus" -> {
                questDatabase.setQuestIfBetter(Quest.PRIMORDIAL, "step2")
                CYRUS_PATTERN.find(html)?.groupValues?.getOrNull(1)?.let { adjective ->
                    QuestSpecialSync.appendCyrusAdjective(preferences, adjective)
                }
                true
            }
            "mother hellseal" -> {
                val prefs = preferences ?: return false
                prefs.setInt("_sealScreeches", max(0, prefs.getInt("_sealScreeches", 0) - 1))
                true
            }
            "travoltron" -> {
                preferences?.setBoolean("_infernoDiscoVisited", false) ?: return false
                true
            }
            "source agent" -> {
                val prefs = preferences ?: return false
                prefs.setInt("sourceAgentsDefeated", max(0, prefs.getInt("sourceAgentsDefeated", 0) - 1))
                true
            }
            else -> false
        }
    }
}
