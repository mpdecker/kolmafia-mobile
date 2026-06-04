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

    // ── detectStep ───────────────────────────────────────────────────────────

    private fun entry(vararg steps: Pair<String, String>) = QuestLogEntry(
        prefKey = "questTest",
        title   = "Test Quest",
        steps   = steps.toList(),
    )

    @Test fun detectStep_bodyMatchesStarted_returnsStarted() {
        val e = entry("started" to "go find the larva", "finished" to "you found it")
        assertEquals("started", QuestLogDatabase.detectStep(e, "Go find the larva."))
    }

    @Test fun detectStep_bodyMatchesFinished_returnsFinished() {
        val e = entry("started" to "go find the larva", "finished" to "you found it")
        assertEquals("finished", QuestLogDatabase.detectStep(e, "You found it!"))
    }

    @Test fun detectStep_bodyMatchesStep1_returnsStep1() {
        val e = entry(
            "started"  to "go talk to bart",
            "step1"    to "explore the cellar",
            "finished" to "done!"
        )
        assertEquals("step1", QuestLogDatabase.detectStep(e, "Explore the cellar now."))
    }

    @Test fun detectStep_noMatch_returnsStarted() {
        val e = entry("started" to "go find", "finished" to "you found")
        assertEquals("started", QuestLogDatabase.detectStep(e, "Something completely different."))
    }

    @Test fun detectStep_stripsHtmlBeforeMatching() {
        val e = entry("started" to "go find the larva", "finished" to "you found it")
        assertEquals("started", QuestLogDatabase.detectStep(e, "<b>Go</b> find <i>the larva</i>."))
    }

    @Test fun detectStep_caseInsensitive() {
        val e = entry("started" to "go find the larva", "finished" to "you found it")
        assertEquals("started", QuestLogDatabase.detectStep(e, "GO FIND THE LARVA NOW"))
    }

    @Test fun detectStep_prefersLaterStepWhenBodyContainsBoth() {
        val e = entry(
            "started"  to "go talk to bart",
            "step1"    to "explore the cellar",
            "finished" to "rats gone"
        )
        val body = "Explore the cellar. Rats gone. Quest complete!"
        assertEquals("finished", QuestLogDatabase.detectStep(e, body))
    }
}
