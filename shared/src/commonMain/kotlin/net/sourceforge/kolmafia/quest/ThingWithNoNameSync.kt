package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestData] Thing with No Name combat-win writer.
 */
object ThingWithNoNameSync {

    const val FURIOUS_STONE = 5448
    const val VANITY_STONE = 5449
    const val LECHEROUS_STONE = 5450
    const val JEALOUSY_STONE = 5451
    const val AVARICE_STONE = 5452
    const val GLUTTONOUS_STONE = 5453

    private val STONES = listOf(
        FURIOUS_STONE,
        VANITY_STONE,
        LECHEROUS_STONE,
        JEALOUSY_STONE,
        AVARICE_STONE,
        GLUTTONOUS_STONE,
    )

    fun apply(
        monster: String,
        won: Boolean,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        ascensionNumber: Int,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (!won || questDatabase == null || preferences == null) return false
        if (!monster.trim().equals("The Thing with No Name", ignoreCase = true)) return false
        STONES.forEach { consumeItem(it, 1) }
        questDatabase.setProgress(Quest.CLUMSINESS, QuestDatabase.UNSTARTED)
        questDatabase.setProgress(Quest.GLACIER, QuestDatabase.UNSTARTED)
        questDatabase.setProgress(Quest.MAELSTROM, QuestDatabase.UNSTARTED)
        preferences.setInt("lastThingWithNoNameDefeated", ascensionNumber)
        return true
    }
}
