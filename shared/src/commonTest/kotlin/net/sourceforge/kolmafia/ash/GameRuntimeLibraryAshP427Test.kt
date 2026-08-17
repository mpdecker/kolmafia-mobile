package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.data.QuestLogDatabase
import net.sourceforge.kolmafia.data.QuestLogEntry
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.quest.QuestLogProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP427Test {

    @Test
    fun revision_phase480() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun questLogProgress_fuzzyDetectStep_matchesLongStepTail() {
        val tail = "y".repeat(100)
        val entry = QuestLogEntry(
            prefKey = Quest.PARTY_FAIR.prefKey,
            title = "Party Fair",
            steps = listOf(
                "started" to "start",
                "step1" to "x$tail",
                "finished" to "done",
            ),
        )
        val step = QuestLogProgress.findQuestProgress(
            Quest.LARVA.prefKey,
            "Party status: $tail",
            entry,
            null,
            null,
        )
        assertEquals("step1", step)
    }

    @Test
    fun questFightRules_partyFairWootsCombat_requestsQuestLogResync() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_questPartyFairQuest", "woots")
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, "step1")
        val result = QuestFightRules.applyCombat(db, "party girl", won = true, preferences = prefs)
        assertFalse(result.advanced)
        assertTrue(result.resyncQuestLogPage1)
    }

    @Test
    fun questLogDatabase_batEntry_loadsFromFixture() {
        QuestLogDatabase.injectForTest(listOf(
            QuestLogEntry(
                prefKey = Quest.BAT.prefKey,
                title = "Ooh, I Think I Smell a Bat.",
                steps = listOf(
                    "started" to "find and defeat the boss bat",
                    "step1" to "continue searching for the boss bat",
                    "finished" to "you have slain the boss bat",
                ),
            ),
        ))
        val entry = QuestLogDatabase.findByTitle("Ooh, I Think I Smell a Bat.")
        assertEquals(Quest.BAT.prefKey, entry?.prefKey)
    }
}
