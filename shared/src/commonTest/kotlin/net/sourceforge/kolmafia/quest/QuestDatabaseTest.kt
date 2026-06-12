package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestDatabaseTest {

    private fun db(): QuestDatabase = QuestDatabase(Preferences(MapSettings()))

    // ── Shared settings instance for cross-db-instance tests ────────────────
    private val sharedSettings = MapSettings()
    private fun sharedDb() = QuestDatabase(Preferences(sharedSettings))

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

    @Test fun allQuestPrefKeysAreUnique() {
        val keys = Quest.entries.map { it.prefKey }
        assertEquals(keys.size, keys.distinct().size,
            "Duplicate prefKeys: ${keys.groupBy { it }.filter { it.value.size > 1 }.keys}")
    }

    // ── validateStep ────────────────────────────────────────────────────────

    @Test fun validateStep_acceptsUnstarted() =
        assertEquals(QuestDatabase.UNSTARTED, QuestDatabase.validateStep("unstarted"))

    @Test fun validateStep_acceptsStarted() =
        assertEquals(QuestDatabase.STARTED, QuestDatabase.validateStep("started"))

    @Test fun validateStep_acceptsFinished() =
        assertEquals(QuestDatabase.FINISHED, QuestDatabase.validateStep("finished"))

    @Test fun validateStep_acceptsStep1() =
        assertEquals("step1", QuestDatabase.validateStep("step1"))

    @Test fun validateStep_acceptsStep42() =
        assertEquals("step42", QuestDatabase.validateStep("step42"))

    @Test fun validateStep_acceptsStep16_5() =
        assertEquals("step16.5", QuestDatabase.validateStep("step16.5"))

    @Test fun validateStep_rejectsGarbage_returnsUnstarted() =
        assertEquals(QuestDatabase.UNSTARTED, QuestDatabase.validateStep("bogus"))

    @Test fun validateStep_rejectsStepWithNoNumber_returnsUnstarted() =
        assertEquals(QuestDatabase.UNSTARTED, QuestDatabase.validateStep("step"))

    // ── setProgressByPrefKey ────────────────────────────────────────────────

    @Test fun setProgressByPrefKey_validStep_writesPreference() {
        val db = sharedDb()
        db.setProgressByPrefKey("questL02Larva", "step1")
        assertTrue(sharedDb().isQuestLaterThan(Quest.LARVA, QuestDatabase.STARTED))
    }

    @Test fun setProgressByPrefKey_invalidStep_writesUnstarted() {
        val db2settings = MapSettings()
        val db = QuestDatabase(Preferences(db2settings))
        db.setProgressByPrefKey("questL02Larva", "garbage")
        assertFalse(QuestDatabase(Preferences(db2settings)).isQuestLaterThan(Quest.LARVA, QuestDatabase.UNSTARTED))
    }
}
