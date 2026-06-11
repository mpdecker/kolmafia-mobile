package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryQuestTest {

    private fun libWithQuest(step: String = QuestDatabase.UNSTARTED): GameRuntimeLibrary {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.LARVA, step)
        return GameRuntimeLibrary(questDatabase = db)
    }

    @Test
    fun questStatus_returnsCurrentStep() {
        val lib = libWithQuest(QuestDatabase.STARTED)
        assertEquals(QuestDatabase.STARTED, outputLib(lib, """print(quest_status("LARVA"));"""))
    }

    @Test
    fun questStatus_acceptsPrefKey() {
        val lib = libWithQuest("step2")
        assertEquals("step2", outputLib(lib, """print(quest_status("questL02Larva"));"""))
    }

    @Test
    fun questStep_returnsOrdinal() {
        val lib = libWithQuest("step3")
        assertEquals("3", outputLib(lib, """print(to_string(quest_step("LARVA")));"""))
    }

    @Test
    fun questFinished_trueWhenFinished() {
        val lib = libWithQuest(QuestDatabase.FINISHED)
        assertEquals("true", outputLib(lib, """print(to_string(quest_finished("LARVA")));"""))
    }

    @Test
    fun setQuestProgress_writesPref() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val lib = GameRuntimeLibrary(questDatabase = db)
        runLib(lib, """set_quest_progress("LARVA", "step5");""")
        assertEquals("step5", db.getProgress(Quest.LARVA))
    }

    @Test
    fun questDatabase_isAtLeast() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.RAT, "step2")
        assertTrue(db.isAtLeast(Quest.RAT, QuestDatabase.STARTED))
        assertTrue(db.isAtLeast(Quest.RAT, "step2"))
    }

    @Test
    fun questDatabase_isFinished() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.BAT, QuestDatabase.FINISHED)
        assertTrue(db.isFinished(Quest.BAT.prefKey))
    }
}
