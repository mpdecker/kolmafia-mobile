package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TelegramChoiceSync

class GameRuntimeLibraryAshP672Test {

    @Test
    fun revision_phase677() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun office_decision2StartsAndPicksNthName() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val visit = """
            <form>
            <input type="submit" value="RE: Easy Case">
            <input type="submit" value="RE: Medium Mystery">
            <input type="submit" value="RE: Hard Puzzle">
            </form>
        """.trimIndent()
        assertTrue(
            TelegramChoiceSync.apply(
                choiceId = TelegramChoiceSync.OFFICE,
                decision = 2,
                html = "post",
                visitHtml = visit,
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.TELEGRAM))
        assertEquals(2, prefs.getInt("lttQuestDifficulty"))
        assertEquals(0, prefs.getInt("lttQuestStageCount"))
        assertEquals("Medium Mystery", prefs.getString("lttQuestName"))
    }

    @Test
    fun office_decision5ClearsQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString("lttQuestName", "old")
        prefs.setInt("lttQuestDifficulty", 3)
        db.setProgress(Quest.TELEGRAM, "step2")
        assertTrue(
            TelegramChoiceSync.apply(
                choiceId = TelegramChoiceSync.OFFICE,
                decision = 5,
                html = "",
                visitHtml = null,
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.TELEGRAM))
        assertEquals(0, prefs.getInt("lttQuestDifficulty"))
        assertEquals(0, prefs.getInt("lttQuestStageCount"))
        assertEquals("", prefs.getString("lttQuestName"))
    }

    @Test
    fun investigation_stepsAdvance() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        TelegramChoiceSync.apply(TelegramChoiceSync.BEGINS, 1, "", null, db, prefs)
        assertEquals("step1", db.getProgress(Quest.TELEGRAM))
        TelegramChoiceSync.apply(TelegramChoiceSync.CONTINUES, 1, "", null, db, prefs)
        assertEquals("step2", db.getProgress(Quest.TELEGRAM))
        TelegramChoiceSync.apply(TelegramChoiceSync.CONTINUES_AGAIN, 1, "", null, db, prefs)
        assertEquals("step3", db.getProgress(Quest.TELEGRAM))
        TelegramChoiceSync.apply(TelegramChoiceSync.CONCLUDES, 1, "", null, db, prefs)
        assertEquals("step4", db.getProgress(Quest.TELEGRAM))
        assertEquals(0, prefs.getInt("lttQuestStageCount"))
    }

    @Test
    fun questChoiceRules_wires1171FromResponseText() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = TelegramChoiceSync.OFFICE,
                responseText = """<input type="submit" value="RE: Desk Job">""",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.TELEGRAM))
        assertEquals("Desk Job", prefs.getString("lttQuestName"))
    }
}
