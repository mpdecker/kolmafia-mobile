package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestDatabaseTest {

    private fun db(): QuestDatabase = QuestDatabase(Preferences(MapSettings()))

    @Test fun unstarted_isNotLaterThanUnstarted() =
        assertFalse(db().isQuestLaterThan(Quest.MEATSMITH, QuestDatabase.UNSTARTED))

    @Test fun started_isLaterThanUnstarted() {
        val db = db(); db.setProgress(Quest.MEATSMITH, QuestDatabase.STARTED)
        assertTrue(db.isQuestLaterThan(Quest.MEATSMITH, QuestDatabase.UNSTARTED))
    }

    @Test fun step1_isLaterThanStarted() {
        val db = db(); db.setProgress(Quest.ARMORER, "step1")
        assertTrue(db.isQuestLaterThan(Quest.ARMORER, QuestDatabase.STARTED))
    }

    @Test fun step4_isLaterThanStep3() {
        val db = db(); db.setProgress(Quest.ARMORER, "step4")
        assertTrue(db.isQuestLaterThan(Quest.ARMORER, "step3"))
    }

    @Test fun step4_isNotLaterThanStep4() {
        val db = db(); db.setProgress(Quest.ARMORER, "step4")
        assertFalse(db.isQuestLaterThan(Quest.ARMORER, "step4"))
    }

    @Test fun finished_isFinished() {
        val db = db(); db.setProgress(Quest.ARMORER, QuestDatabase.FINISHED)
        assertTrue(db.isQuestFinished(Quest.ARMORER))
    }

    @Test fun step4_isNotFinished() {
        val db = db(); db.setProgress(Quest.ARMORER, "step4")
        assertFalse(db.isQuestFinished(Quest.ARMORER))
    }
}
