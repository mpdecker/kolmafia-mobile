package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PartyFairChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP673Test {

    @Test
    fun revision_phase677() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_1322SetsQuestFromText() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            PartyFairChoiceSync.applyVisit(
                PartyFairChoiceSync.BEGINNING,
                "talk to him and help him get more booze",
                prefs,
            ),
        )
        assertEquals("booze", prefs.getString("_questPartyFairQuest"))
        PartyFairChoiceSync.applyVisit(
            PartyFairChoiceSync.PAUSED,
            "paused",
            prefs,
        )
        assertEquals(7, prefs.getInt("encountersUntilNEPChoice"))
    }

    @Test
    fun beginning_decision1StartsBoozeAndSetsPartyHard() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString("_questPartyFairQuest", "booze")
        assertTrue(
            PartyFairChoiceSync.apply(
                choiceId = PartyFairChoiceSync.BEGINNING,
                decision = 1,
                html = "",
                questDatabase = db,
                preferences = prefs,
                hasItemEquipped = { it == PartyFairChoiceSync.PARTY_HARD_T_SHIRT },
            ),
        )
        assertTrue(prefs.getBoolean("_partyHard"))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.PARTY_FAIR))
        assertEquals(7, prefs.getInt("encountersUntilNEPChoice"))
    }

    @Test
    fun beginning_partiersHardSets100() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString("_questPartyFairQuest", "partiers")
        PartyFairChoiceSync.apply(
            choiceId = PartyFairChoiceSync.BEGINNING,
            decision = 1,
            html = "",
            questDatabase = db,
            preferences = prefs,
            hasItemEquipped = { it == PartyFairChoiceSync.PARTY_HARD_T_SHIRT },
        )
        assertEquals("step1", db.getProgress(Quest.PARTY_FAIR))
        assertEquals(100, prefs.getInt("_questPartyFairProgress"))
    }

    @Test
    fun beginning_decision2ClearsPrefs() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString("_questPartyFair", "started")
        prefs.setString("_questPartyFairQuest", "woots")
        prefs.setString("_questPartyFairProgress", "10")
        PartyFairChoiceSync.apply(
            choiceId = PartyFairChoiceSync.BEGINNING,
            decision = 2,
            html = "",
            questDatabase = db,
            preferences = prefs,
        )
        assertEquals("", prefs.getString("_questPartyFair"))
        assertEquals("", prefs.getString("_questPartyFairQuest"))
        assertEquals("", prefs.getString("_questPartyFairProgress"))
    }

    @Test
    fun roomWithAView_wootsCapAndDress() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("_questPartyFairProgress", 90)
        val consumed = mutableListOf<Pair<Int, Int>>()
        PartyFairChoiceSync.apply(
            choiceId = PartyFairChoiceSync.ROOM_WITH_A_VIEW,
            decision = 5,
            html = "",
            questDatabase = db,
            preferences = prefs,
            consumeItem = { id, qty -> consumed.add(id to qty) },
        )
        assertEquals(100, prefs.getInt("_questPartyFairProgress"))
        assertEquals("step2", db.getProgress(Quest.PARTY_FAIR))
        assertTrue(consumed.contains(PartyFairChoiceSync.VERY_SMALL_RED_DRESS to 1))
    }

    @Test
    fun goneKitchin_geraldineSetsProgressAndStep2() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = "Geraldine wants 3<table><img onclick=\"descitem(4242)\"></table>"
        PartyFairChoiceSync.apply(
            choiceId = PartyFairChoiceSync.GONE_KITCHIN,
            decision = 3,
            html = html,
            questDatabase = db,
            preferences = prefs,
            itemCount = { 5 },
            itemIdFromDesc = { if (it == "4242") 9960 else null },
        )
        assertEquals("3 9960", prefs.getString("_questPartyFairProgress"))
        assertEquals("step2", db.getProgress(Quest.PARTY_FAIR))
    }

    @Test
    fun paused_decrementsEncounters() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("encountersUntilNEPChoice", 7)
        prefs.setInt("_neverendingPartyFreeTurns", 9)
        PartyFairChoiceSync.apply(
            choiceId = PartyFairChoiceSync.PAUSED,
            decision = 1,
            html = "",
            questDatabase = db,
            preferences = prefs,
        )
        assertEquals(6, prefs.getInt("encountersUntilNEPChoice"))
        assertEquals(10, prefs.getInt("_neverendingPartyFreeTurns"))
        assertFalse(
            PartyFairChoiceSync.apply(
                choiceId = PartyFairChoiceSync.PAUSED,
                decision = 5,
                html = "",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(6, prefs.getInt("encountersUntilNEPChoice"))
    }

    @Test
    fun questChoiceRules_wires1323() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString("_questPartyFairQuest", "woots")
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = PartyFairChoiceSync.ALL_DONE,
                responseText = "All Done!",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.PARTY_FAIR))
        assertEquals("", prefs.getString("_questPartyFairQuest"))
    }
}
