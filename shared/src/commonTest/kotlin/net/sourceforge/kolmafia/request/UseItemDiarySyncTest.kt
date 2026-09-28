package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class UseItemDiarySyncTest {

    private fun quests(): Pair<Preferences, QuestDatabase> {
        val prefs = Preferences(MapSettings())
        return prefs to QuestDatabase(prefs)
    }

    @Test
    fun diaryPage_advancesMacGuffinAndStartsCouncilQuests() {
        val (prefs, quests) = quests()
        UseItemDiarySync.handle("Father's Diary", quests, prefs, ascensions = 4)
        assertEquals("step2", quests.getProgress(Quest.MACGUFFIN))
        assertEquals(QuestDatabase.FINISHED, quests.getProgress(Quest.BLACK))
        assertEquals(QuestDatabase.STARTED, quests.getProgress(Quest.DESERT))
        assertEquals(QuestDatabase.STARTED, quests.getProgress(Quest.MANOR))
        assertEquals(QuestDatabase.STARTED, quests.getProgress(Quest.SHEN))
        assertEquals(QuestDatabase.STARTED, quests.getProgress(Quest.RON))
        assertEquals(QuestDatabase.STARTED, quests.getProgress(Quest.WORSHIP))
    }

    @Test
    fun templeUnlockedThisAscension_setsWorshipStep1() {
        val (prefs, quests) = quests()
        prefs.setInt("lastTempleUnlock", 4)
        UseItemDiarySync.handle("Diary", quests, prefs, ascensions = 4)
        assertEquals("step1", quests.getProgress(Quest.WORSHIP))
    }

    @Test
    fun otherText_leavesQuestsUnstarted() {
        val (prefs, quests) = quests()
        UseItemDiarySync.handle("The page is blank.", quests, prefs, ascensions = 1)
        assertEquals(QuestDatabase.UNSTARTED, quests.getProgress(Quest.MACGUFFIN))
        assertEquals(QuestDatabase.UNSTARTED, quests.getProgress(Quest.BLACK))
        assertEquals(QuestDatabase.UNSTARTED, quests.getProgress(Quest.WORSHIP))
    }

    @Test
    fun laterProgress_doesNotRegress() {
        val (prefs, quests) = quests()
        quests.setProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED)
        quests.setProgress(Quest.WORSHIP, "step3")
        prefs.setInt("lastTempleUnlock", 2)
        UseItemDiarySync.handle("Diary", quests, prefs, ascensions = 2)
        assertEquals(QuestDatabase.FINISHED, quests.getProgress(Quest.MACGUFFIN))
        assertEquals("step3", quests.getProgress(Quest.WORSHIP))
        assertEquals(QuestDatabase.FINISHED, quests.getProgress(Quest.BLACK))
    }
}
