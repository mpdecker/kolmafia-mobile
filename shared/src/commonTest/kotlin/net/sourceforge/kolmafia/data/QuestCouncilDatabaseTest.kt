package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class QuestCouncilDatabaseTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun parseForTest_readsPrefStepAndTextVariants() {
        val fixture = """
            questL02Larva	started	We require your aid, Adventurer.	We still need a mosquito larva.
            questL02Larva	finished	Thanks for the larva, Adventurer.
        """.trimIndent()
        val entries = QuestCouncilDatabase.parseForTest(fixture)
        assertEquals(2, entries.size)
        assertEquals("questL02Larva", entries[0].prefKey)
        assertEquals("started", entries[0].step)
        assertEquals(2, entries[0].texts.size)
    }

    @Test
    fun handleCouncilText_advancesLarvaStarted() {
        QuestCouncilDatabase.injectForTest(
            listOf(
                QuestCouncilEntry(
                    prefKey = Quest.LARVA.prefKey,
                    step = QuestDatabase.STARTED,
                    texts = listOf(
                        "We require your aid, Adventurer. We need a mosquito larva.",
                    ),
                ),
            ),
        )
        val testPrefs = prefs()
        val db = QuestDatabase(testPrefs)
        val html = "<p>We require your aid, Adventurer. We need a mosquito larva.</p>"
        QuestCouncilDatabase.handleCouncilText(html, db)
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.LARVA))
    }

    @Test
    fun handleCouncilText_doesNotRegressFinishedQuest() {
        QuestCouncilDatabase.injectForTest(
            listOf(
                QuestCouncilEntry(
                    prefKey = Quest.LARVA.prefKey,
                    step = QuestDatabase.STARTED,
                    texts = listOf("We still need a mosquito larva."),
                ),
            ),
        )
        val testPrefs = prefs()
        val db = QuestDatabase(testPrefs)
        db.setProgress(Quest.LARVA, QuestDatabase.FINISHED)
        QuestCouncilDatabase.handleCouncilText(
            "<p>We still need a mosquito larva, Adventurer.</p>",
            db,
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.LARVA))
    }

    @Test
    fun handleCouncilText_advancesLarvaFinished() {
        QuestCouncilDatabase.injectForTest(
            listOf(
                QuestCouncilEntry(
                    prefKey = Quest.LARVA.prefKey,
                    step = QuestDatabase.FINISHED,
                    texts = listOf("Thanks for the larva, Adventurer."),
                ),
            ),
        )
        val testPrefs = prefs()
        val db = QuestDatabase(testPrefs)
        db.setProgress(Quest.LARVA, QuestDatabase.STARTED)
        QuestCouncilDatabase.handleCouncilText(
            "<p>Thanks for the larva, Adventurer. We'll put this to good use.</p>",
            db,
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.LARVA))
    }
}
