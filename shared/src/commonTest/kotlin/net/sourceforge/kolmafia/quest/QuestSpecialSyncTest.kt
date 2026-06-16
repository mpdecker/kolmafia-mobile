package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestSpecialSyncTest {

    @Test
    fun apply_telegramAdvancesFancyManSteps() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TELEGRAM, QuestDatabase.STARTED)
        assertTrue(
            QuestSpecialSync.apply(
                "Ask around the Rough Diamond Saloon to see if anybody has seen Jeff the Fancy Dude.",
                db,
                prefs,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.TELEGRAM))
        assertEquals("Missing: Fancy Man", prefs.getString("lttQuestName", ""))
        assertEquals(1, prefs.getInt("lttQuestDifficulty", 0))
    }

    @Test
    fun apply_telegramAdvancesToStep4() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TELEGRAM, "step3")
        assertTrue(QuestSpecialSync.apply("Defeat Jeff the Fancy Skeleton.", db, prefs))
        assertEquals("step4", db.getProgress(Quest.TELEGRAM))
    }

    @Test
    fun apply_partyFairTrashSetsSubQuestAndProgress() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED)
        assertTrue(QuestSpecialSync.apply("Clean up the trash: Trash left: ~1,234 pieces.", db, prefs))
        assertEquals("step1", db.getProgress(Quest.PARTY_FAIR))
        assertEquals("trash", prefs.getString("_questPartyFairQuest", ""))
        assertEquals("1234", prefs.getString("_questPartyFairProgress", ""))
    }

    @Test
    fun apply_partyFairBoozeBackyard() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, QuestDatabase.UNSTARTED)
        db.setProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED)
        assertTrue(QuestSpecialSync.apply("Check the backyard for clues.", db, prefs))
        assertEquals("started", db.getProgress(Quest.PARTY_FAIR))
        assertEquals("booze", prefs.getString("_questPartyFairQuest", ""))
    }

    @Test
    fun apply_oracleSetsTargetAndStartsQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val text = "The Oracle wants you to find <b>absence of a spoon</b> somewhere."
        assertTrue(QuestSpecialSync.apply(text, db, prefs))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.ORACLE))
        assertEquals("absence of a spoon", prefs.getString("sourceOracleTarget", ""))
    }

    @Test
    fun apply_ghostSetsLocation() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.GHOST, QuestDatabase.STARTED)
        val text = "Find the ghost in <b>The Spooky Forest</b>."
        assertTrue(QuestSpecialSync.apply(text, db, prefs))
        assertEquals("The Spooky Forest", prefs.getString("ghostLocation", ""))
    }

    @Test
    fun apply_newYouSetsSkillCounters() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.NEW_YOU, QuestDatabase.STARTED)
        val text = "Looks like you've cast Wave of Sauce during 2 of the required 5 encounters with a wumpus!"
        assertTrue(QuestSpecialSync.apply(text, db, prefs))
        assertEquals("Wave of Sauce", prefs.getString("_newYouQuestSkill", ""))
        assertEquals("2", prefs.getString("_newYouQuestSharpensDone", ""))
        assertEquals("5", prefs.getString("_newYouQuestSharpensToDo", ""))
        assertEquals("wumpus", prefs.getString("_newYouQuestMonster", ""))
    }

    @Test
    fun apply_shenSetsQuestItem() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.SHEN, QuestDatabase.STARTED)
        assertTrue(QuestSpecialSync.apply("Recover Talisman o' Namsilat from Shen Copperhead.", db, prefs))
        assertEquals("Talisman o' Namsilat", prefs.getString("shenQuestItem", ""))
    }

    @Test
    fun apply_doctorBagAcquireAndDeliver() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(QuestSpecialSync.apply("Acquire a bottle of gin.", db, prefs))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.DOCTOR_BAG))
        assertEquals("bottle of gin", prefs.getString("doctorBagQuestItem", ""))
        val deliver = "Take a bottle of gin to the patient in <a href=place.php><b>The Sleazy Back Alley</b></a>."
        assertTrue(QuestSpecialSync.apply(deliver, db, prefs))
        assertEquals("step1", db.getProgress(Quest.DOCTOR_BAG))
        assertEquals("The Sleazy Back Alley", prefs.getString("doctorBagQuestLocation", ""))
    }

    @Test
    fun apply_guzzlrAcquireAndDeliver() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestSpecialSync.apply("Acquire a bottle of rum for your Guzzlr client.", db, prefs),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.GUZZLR))
        assertEquals("bottle of rum", prefs.getString("guzzlrQuestBooze", ""))
        val deliver = "Deliver the bottle of rum to your Guzzlr client: Gerald in The Sleazy Back Alley."
        assertTrue(QuestSpecialSync.apply(deliver, db, prefs))
        assertEquals("step1", db.getProgress(Quest.GUZZLR))
        assertEquals("Gerald", prefs.getString("guzzlrQuestClient", ""))
    }

    @Test
    fun apply_doctorBagDeliveryCompletesAndClearsPrefs() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.DOCTOR_BAG, "step1")
        prefs.setString("doctorBagQuestItem", "bottle of gin")
        prefs.setString("doctorBagQuestLocation", "The Sleazy Back Alley")
        val text = "One of the five green lights on the doctor bag lights up. The bag has been permanently upgraded."
        assertTrue(QuestSpecialSync.apply(text, db, prefs))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.DOCTOR_BAG))
        assertEquals("", prefs.getString("doctorBagQuestItem", "x"))
        assertEquals("", prefs.getString("doctorBagQuestLocation", "x"))
        assertEquals(1, prefs.getInt("doctorBagQuestLights", 0))
        assertEquals(1, prefs.getInt("doctorBagUpgrades", 0))
    }

    @Test
    fun apply_guzzlrAbandonClearsPrefs() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.GUZZLR, "step1")
        prefs.setString("guzzlrQuestBooze", "bottle of rum")
        prefs.setString("guzzlrQuestClient", "Gerald")
        assertTrue(QuestSpecialSync.abandonGuzzlr(db, prefs))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.GUZZLR))
        assertTrue(prefs.getBoolean("_guzzlrQuestAbandoned"))
        assertEquals("", prefs.getString("guzzlrQuestBooze", "x"))
        assertEquals("", prefs.getString("guzzlrQuestClient", "x"))
    }

    @Test
    fun apply_guzzlrDeliveryIncrementsBronzeCounter() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.GUZZLR, "step1")
        prefs.setString("guzzlrQuestTier", "bronze")
        prefs.setString("guzzlrQuestBooze", "bottle of rum")
        assertTrue(
            QuestSpecialSync.apply(
                "You finally manage to track down Gerald and deliver the booze.",
                db,
                prefs,
            ),
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.GUZZLR))
        assertEquals(1, prefs.getInt("guzzlrBronzeDeliveries", 0))
        assertEquals("", prefs.getString("guzzlrQuestBooze", "x"))
    }

    @Test
    fun apply_primordialAdvancesThroughSoupSteps() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestSpecialSync.apply(
                "You remember floating aimlessly in the Primordial Soup. You wanted to do it some more.",
                db,
                prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.PRIMORDIAL))
        assertTrue(
            QuestSpecialSync.apply(
                "You remember finding your way to a higher, warmer, oranger part of the Primordial Soup.",
                db,
                prefs,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.PRIMORDIAL))
    }

    @Test
    fun apply_competitionSetsContestantPrefs() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(QuestSpecialSync.apply("Contest #3: 12 competitor", db, prefs))
        assertEquals(12, prefs.getInt("nsContestants3", -1))
    }

    @Test
    fun apply_partyFairMeatBill() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED)
        assertTrue(QuestSpecialSync.apply("Remaining bill: 50,000 Meat", db, prefs))
        assertEquals("meat", prefs.getString("_questPartyFairQuest", ""))
        assertEquals("50000", prefs.getString("_questPartyFairProgress", ""))
    }

    @Test
    fun apply_partyFairBoozeResolvesItemId() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED)
        val gameDb = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                1234, name, "", "", net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                emptySet(), emptySet(), 0, null,
            )
        }
        val text = "Get 3 bottle of gin for Gerald at the bar."
        assertTrue(QuestSpecialSync.apply(text, db, prefs, gameDb))
        assertEquals("booze", prefs.getString("_questPartyFairQuest", ""))
        assertEquals("3 1234", prefs.getString("_questPartyFairProgress", ""))
    }

    @Test
    fun apply_guzzlrPlatinumCocktailSetByItemId() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.GUZZLR, QuestDatabase.STARTED)
        val gameDb = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                10542, name, "", "", net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                emptySet(), emptySet(), 0, null,
            )
        }
        val deliver = "Deliver the fancy cocktail to your Guzzlr client: Pat in The Sleazy Back Alley."
        assertTrue(QuestSpecialSync.apply(deliver, db, prefs, gameDb))
        assertEquals("platinum", prefs.getString("guzzlrQuestTier", ""))
        assertEquals("Guzzlr cocktail set", prefs.getString("guzzlrQuestBooze", ""))
    }

    @Test
    fun apply_peakStatusAdvancesMacguffin() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.MACGUFFIN, QuestDatabase.STARTED)
        assertTrue(
            QuestSpecialSync.apply(
                "Investigate A-Boo Peak. It is currently 42%.",
                db,
                prefs,
            ),
        )
        assertEquals("step2", db.getProgress(Quest.MACGUFFIN))
        assertEquals(42, prefs.getInt("booPeakProgress", 0))
    }

    @Test
    fun apply_peakStatusBooPeakCheckOutSetsStartedProgress() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.MACGUFFIN, QuestDatabase.STARTED)
        assertTrue(
            QuestSpecialSync.apply("You should check out A-Boo Peak.", db, prefs),
        )
        assertEquals(100, prefs.getInt("booPeakProgress", 0))
    }

    @Test
    fun apply_peakStatusCompletesAllPeaksToStep3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.MACGUFFIN, "step2")
        prefs.setBoolean("booPeakLit", true)
        prefs.setInt("twinPeakProgress", 15)
        assertTrue(
            QuestSpecialSync.apply("You lit the fire on Oil Peak.", db, prefs),
        )
        assertEquals("step3", db.getProgress(Quest.MACGUFFIN))
        assertTrue(prefs.getBoolean("oilPeakLit", false))
    }

    @Test
    fun apply_partyFairDjAndReturn() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED)
        assertTrue(QuestSpecialSync.apply("Meat for the DJ: Remaining bill: 12,345 Meat", db, prefs))
        assertEquals("dj", prefs.getString("_questPartyFairQuest", ""))
        assertEquals("12345", prefs.getString("_questPartyFairProgress", ""))
        prefs.setString("_questPartyFairQuest", "woots")
        assertTrue(QuestSpecialSync.apply("Return to the party and check hype.", db, prefs))
        assertEquals("step2", db.getProgress(Quest.PARTY_FAIR))
        assertEquals("100", prefs.getString("_questPartyFairProgress", ""))
    }

    @Test
    fun apply_hippyFratSoldierCounts() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.HIPPY_FRAT, QuestDatabase.STARTED)
        assertTrue(
            QuestSpecialSync.apply(
                "Remaining soldiers: 300 hippies, 250 frat boys.",
                db,
                prefs,
            ),
        )
        assertEquals(33, prefs.getInt("hippiesDefeated", 0))
        assertEquals(83, prefs.getInt("fratboysDefeated", 0))
    }

    @Test
    fun apply_telegramAdvancesSheriffWanted() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TELEGRAM, QuestDatabase.STARTED)
        assertTrue(
            QuestSpecialSync.apply(
                "Fight your way to the sheriff's office and apply for the job.",
                db,
                prefs,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.TELEGRAM))
        assertEquals("Sheriff Wanted", prefs.getString("lttQuestName", ""))
        assertEquals(2, prefs.getInt("lttQuestDifficulty", 0))
    }

    @Test
    fun apply_telegramAdvancesManyChildren() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TELEGRAM, "step3")
        assertTrue(QuestSpecialSync.apply("Defeat Clara.", db, prefs))
        assertEquals("step4", db.getProgress(Quest.TELEGRAM))
        assertEquals("Missing: Many Children", prefs.getString("lttQuestName", ""))
        assertEquals(3, prefs.getInt("lttQuestDifficulty", 0))
    }

    @Test
    fun apply_finalQuestAdvancesHedgeMazeStep() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, "step4")
        assertTrue(
            QuestSpecialSync.apply(
                "Make your way through the treacherous hedge maze at the Naughty Sorceress' Tower.",
                db,
                prefs,
            ),
        )
        assertEquals("step5", db.getProgress(Quest.FINAL))
    }

    @Test
    fun apply_finalQuestAdvancesConfrontStep() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, "step11")
        assertTrue(QuestSpecialSync.apply("Confront the Naughty Sorceress.", db, prefs))
        assertEquals("step12", db.getProgress(Quest.FINAL))
    }

    @Test
    fun apply_rufusEntityQuestLog() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val text = "Rufus wants you to go into a Shadow Rift and defeat a shadow scythe."
        assertTrue(QuestSpecialSync.apply(text, db, prefs))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.RUFUS))
        assertEquals("entity", prefs.getString(Preferences.RUFUS_QUEST_TYPE, ""))
        assertEquals("shadow scythe", prefs.getString(Preferences.RUFUS_QUEST_TARGET, ""))
    }
}
