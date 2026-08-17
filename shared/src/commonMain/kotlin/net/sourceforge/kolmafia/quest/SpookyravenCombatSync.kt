package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [QuestManager] Lord Spookyraven / writing desk combat writers. */
object SpookyravenCombatSync {

    private const val LORD_SPOOKYRAVEN = "lord spookyraven"
    private const val WRITING_DESK = "writing desk"
    const val SPOOKYRAVEN_NECKLACE = 7303

    fun applyCombatWin(
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        monster: String,
        won: Boolean,
        hasItemId: (Int) -> Boolean = { false },
    ): Boolean {
        if (questDatabase == null || preferences == null || !won || monster.isBlank()) return false
        val name = monster.trim().lowercase()
        return when (name) {
            LORD_SPOOKYRAVEN -> {
                questDatabase.setProgress(Quest.MANOR, QuestDatabase.FINISHED)
                true
            }
            WRITING_DESK -> applyWritingDesk(questDatabase, preferences, hasItemId)
            else -> false
        }
    }

    private fun applyWritingDesk(
        questDatabase: QuestDatabase,
        preferences: Preferences,
        hasItemId: (Int) -> Boolean,
    ): Boolean {
        if (!questDatabase.isAtLeast(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.STARTED)) return false
        if (hasItemId(SPOOKYRAVEN_NECKLACE)) return false
        if (questDatabase.isQuestFinished(Quest.SPOOKYRAVEN_NECKLACE)) return false
        val current = preferences.getInt("writingDesksDefeated", 0)
        if (current >= 5) return false
        preferences.setInt("writingDesksDefeated", current + 1)
        return true
    }
}
