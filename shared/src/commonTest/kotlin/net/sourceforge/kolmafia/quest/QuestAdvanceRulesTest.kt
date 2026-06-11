package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestAdvanceRulesTest {

    @Test
    fun apply_advancesLarvaOnItemAcquire() {
        val db = QuestDatabase(Preferences(MapSettings()))
        val text = "You acquire an item: larva"
        assertTrue(QuestAdvanceRules.apply(text, db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.LARVA))
    }

    @Test
    fun apply_doesNotRegressFinishedQuest() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.LARVA, QuestDatabase.FINISHED)
        assertFalse(QuestAdvanceRules.apply("You acquire an item: larva", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.LARVA))
    }

    @Test
    fun apply_macguffinUnlockSetsFinished() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("You have unlocked the Forbidden Zone", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.MACGUFFIN))
    }

    @Test
    fun apply_toppingOnAglet() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("You acquire an item: rainbow aglet", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.TOPPING))
    }

    @Test
    fun apply_goblinFinishedOnKingSlain() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("You have slain the Goblin King", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.GOBLIN))
    }

    @Test
    fun apply_batFinishedOnBossBatSlain() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("You have slain the Boss Bat. Huzzah!", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.BAT))
    }

    @Test
    fun apply_manorFinishedOnEyeOfEd() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("You acquire an item: Eye of Ed", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.MANOR))
    }

    @Test
    fun apply_noMatchReturnsFalse() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertFalse(QuestAdvanceRules.apply("You fight a seal.", db))
    }
}
