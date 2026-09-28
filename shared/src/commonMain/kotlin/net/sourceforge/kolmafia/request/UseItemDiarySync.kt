package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/**
 * Desktop `UseItemRequest.handleDiary`: a diary page that contains “Diary” advances the
 * MacGuffin chain and starts the related council quests. Worship reaches step 1 when the
 * Hidden Temple was already unlocked this ascension.
 */
object UseItemDiarySync {
    fun handle(
        responseText: String,
        quests: QuestDatabase?,
        preferences: Preferences? = null,
        ascensions: Int = 0,
    ) {
        if (quests == null || !responseText.contains("Diary")) return
        quests.setQuestIfBetter(Quest.MACGUFFIN, "step2")
        quests.setQuestIfBetter(Quest.BLACK, QuestDatabase.FINISHED)
        quests.setQuestIfBetter(Quest.DESERT, QuestDatabase.STARTED)
        quests.setQuestIfBetter(Quest.MANOR, QuestDatabase.STARTED)
        quests.setQuestIfBetter(Quest.SHEN, QuestDatabase.STARTED)
        quests.setQuestIfBetter(Quest.RON, QuestDatabase.STARTED)
        val templeUnlocked = (preferences?.getInt("lastTempleUnlock", -1) ?: -1) == ascensions
        quests.setQuestIfBetter(
            Quest.WORSHIP,
            if (templeUnlocked) "step1" else QuestDatabase.STARTED,
        )
    }
}
