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
    fun apply_larvaFinishedOnCouncilTurnIn() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.LARVA, QuestDatabase.STARTED)
        assertTrue(QuestAdvanceRules.apply("Thanks for the larva, Adventurer.", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.LARVA))
    }

    @Test
    fun apply_ratFinishedOnTavernComplete() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.RAT, QuestDatabase.STARTED)
        assertTrue(QuestAdvanceRules.apply("You've solved the rat problem at the Typical Tavern. Way to go!", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.RAT))
    }

    @Test
    fun apply_batStartedOnCouncilAssignment() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("You must slay the Boss Bat in the Bat Hole.", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.BAT))
    }

    @Test
    fun apply_batFinishedOnCouncilTurnIn() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.BAT, QuestDatabase.STARTED)
        assertTrue(QuestAdvanceRules.apply("Well done!  You have slain the Boss Bat.", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.BAT))
    }

    @Test
    fun apply_goblinFinishedOnCouncilThanks() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.GOBLIN, QuestDatabase.STARTED)
        assertTrue(QuestAdvanceRules.apply("Thank you for slaying the Goblin King, Adventurer.", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.GOBLIN))
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
    fun apply_palindomeFinishedOnStaffOfFats() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("recovered the long-lost Staff of Fats", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.PALINDOME))
    }

    @Test
    fun apply_worshipFinishedOnAmulet() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("claimed his ancient amulet", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.WORSHIP))
    }

    @Test
    fun apply_macguffinStartedOnBlackMarket() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("Your first step is to find the Black Market", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.MACGUFFIN))
    }

    @Test
    fun apply_pyramidFinishedOnEdFallen() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("Ed the Undying has fallen", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.PYRAMID))
    }

    @Test
    fun apply_noMatchReturnsFalse() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertFalse(QuestAdvanceRules.apply("You fight a seal.", db))
    }

    @Test
    fun apply_seaOldGuyStartedOnSignal() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("Talk to the old guy by the sea.", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.SEA_OLD_GUY))
    }

    @Test
    fun apply_pirateRealmStartedOnSignal() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("Welcome to the Pirate Realm!", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.PIRATEREALM))
    }

    @Test
    fun apply_telegramStartedOnSignal() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("A telegram for you, Adventurer.", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.TELEGRAM))
    }
}
