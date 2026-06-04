package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.QuestLogDatabase
import net.sourceforge.kolmafia.data.QuestLogEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestLogDatabaseTest {

    private fun loadFixture(text: String): Map<String, QuestLogEntry> =
        QuestLogDatabase.parseForTest(text)

    @Test fun parse_skipsVersionAndCommentLines() {
        val entries = loadFixture("1\n# comment\n")
        assertTrue(entries.isEmpty())
    }

    @Test fun parse_singleEntry_twoTexts_startedAndFinished() {
        val entries = loadFixture(
            "questL02Larva\tFind a Larva\tGo find the larva.\tYou found the larva!\n"
        )
        val entry = entries["find a larva"]
        assertNotNull(entry)
        assertEquals("questL02Larva", entry.prefKey)
        assertEquals(2, entry.steps.size)
        assertEquals("started", entry.steps[0].first)
        assertEquals("go find the larva.", entry.steps[0].second)
        assertEquals("finished", entry.steps[1].first)
        assertEquals("you found the larva!", entry.steps[1].second)
    }

    @Test fun parse_threeTexts_hasStep1() {
        val entries = loadFixture(
            "questL03Rat\tSmell a Rat\tTalk to Bart.\tExplore the cellar.\tDone!\n"
        )
        val entry = entries["smell a rat"]
        assertNotNull(entry)
        assertEquals(3, entry.steps.size)
        assertEquals("started",  entry.steps[0].first)
        assertEquals("step1",    entry.steps[1].first)
        assertEquals("finished", entry.steps[2].first)
    }

    @Test fun parse_singleText_finishedOnly() {
        val entries = loadFixture("questM05Toot\tToot Tutorial\tYou are done.\n")
        val entry = entries["toot tutorial"]
        assertNotNull(entry)
        assertEquals(1, entry.steps.size)
        assertEquals("finished", entry.steps[0].first)
    }

    @Test fun parse_titleLookupCaseInsensitive() {
        val entries = loadFixture("questL02Larva\tFind A Larva\tGo.\tDone!\n")
        assertNotNull(entries["find a larva"])
    }

    @Test fun parse_missingColumns_skipped() {
        val entries = loadFixture("questL02Larva\tTitle Only\n")
        assertNull(entries["title only"])
    }

    @Test fun parse_stripsHtmlTagsFromStepText() {
        val entries = loadFixture(
            "questL02Larva\tFind a Larva\t<b>Go</b> find <i>the</i> larva.\tDone!\n"
        )
        val entry = entries["find a larva"]!!
        assertEquals("go find the larva.", entry.steps[0].second)
    }
}
