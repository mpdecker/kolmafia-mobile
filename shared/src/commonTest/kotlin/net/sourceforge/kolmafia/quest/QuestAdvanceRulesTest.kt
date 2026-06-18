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
        assertTrue(QuestAdvanceRules.apply("I lost my favorite boot in the ocean.", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.SEA_OLD_GUY))
    }

    @Test
    fun apply_meatcarStartedOnGuildSignal() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("Welcome to Degrassi Knoll!", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.MEATCAR))
    }

    @Test
    fun apply_egoStartedOnGuildSignal() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("the location of the Cemetary is", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.EGO))
    }

    @Test
    fun apply_egoTowerProgressionSignals() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.EGO, "step2")
        assertTrue(QuestAdvanceRules.apply("You've unlocked Fernswarthy's tower.", db))
        assertEquals("step3", db.getProgress(Quest.EGO))
        assertTrue(QuestAdvanceRules.apply("You found some stairs in Fernswarthy's tower.", db))
        assertEquals("step4", db.getProgress(Quest.EGO))
        assertTrue(QuestAdvanceRules.apply("You found a trapdoor to Fernswarthy's basement.", db))
        assertEquals("step5", db.getProgress(Quest.EGO))
        assertTrue(QuestAdvanceRules.apply("You found some kind of dusty old book.", db))
        assertEquals("step6", db.getProgress(Quest.EGO))
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

    @Test
    fun stepOrdinal_supportsFractionalSteps() {
        assertTrue(QuestDatabase.stepOrdinal("step16.5") > QuestDatabase.stepOrdinal("step16"))
        assertTrue(QuestDatabase.stepOrdinal("step17") > QuestDatabase.stepOrdinal("step16.5"))
    }

    @Test
    fun apply_citadelStep1OnSignSignal() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.CITADEL, QuestDatabase.STARTED)
        assertTrue(QuestAdvanceRules.apply("It's A Sign! You should visit Whitey's Grove.", db))
        assertEquals("step1", db.getProgress(Quest.CITADEL))
    }

    @Test
    fun apply_doctorStartedOnGalaktikSignal() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("You meet Doc Galaktik in the store.", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.DOCTOR))
    }

    @Test
    fun apply_hiddenApartmentStartsCurses() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("Welcome to the Hidden Apartment.", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.CURSES))
    }

    @Test
    fun apply_worshipStep2OnPomPomsSignal() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.WORSHIP, QuestDatabase.STARTED)
        assertTrue(QuestAdvanceRules.apply("put your pom-poms down", db))
        assertEquals("step2", db.getProgress(Quest.WORSHIP))
    }

    @Test
    fun apply_islandWarStartedOnCouncilTension() {
        val db = QuestDatabase(Preferences(MapSettings()))
        val text = "The Council has gotten word of tensions building between the hippies and the frat boys"
        assertTrue(QuestAdvanceRules.apply(text, db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.ISLAND_WAR))
    }

    @Test
    fun apply_islandWarFinishedOnHippyVictory() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.ISLAND_WAR, "step1")
        assertTrue(QuestAdvanceRules.apply("You led the filthy hippies to victory", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.ISLAND_WAR))
    }

    @Test
    fun apply_ronFinishedOnTalismanRecovery() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.RON, QuestDatabase.STARTED)
        val text = "You recovered half of the Talisman o' Namsilat from Ron Copperhead. Brilliant!"
        assertTrue(QuestAdvanceRules.apply(text, db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.RON))
    }

    @Test
    fun apply_warehouseFinishedOnMacGuffin() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.WAREHOUSE, QuestDatabase.STARTED)
        assertTrue(QuestAdvanceRules.apply("You retrieved the Holy MacGuffin!", db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.WAREHOUSE))
    }

    @Test
    fun apply_darkStartedOnCaveMarked() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestAdvanceRules.apply("marked your map with the location of a cave", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.DARK))
    }

    @Test
    fun apply_pirateFinishedOnBelowdecksScam() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.PIRATE, "step6")
        val text = "Oh, and also you've managed to scam your way belowdecks, which is cool."
        assertTrue(QuestAdvanceRules.apply(text, db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.PIRATE))
    }
}
