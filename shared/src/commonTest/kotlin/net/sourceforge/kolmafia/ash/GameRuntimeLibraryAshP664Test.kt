package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportRadioChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP664Test {

    @Test
    fun revision_phase665() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun radio_startsEveWithDirections() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportRadioChoiceSync.apply(
                html = "You use your best paramilitary-sounding radio lingo and receive a navigation protocol.",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.EVE))
        assertEquals("LLRLR0", prefs.getString("EVEDirections"))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.JUNGLE_PUN))
    }

    @Test
    fun radio_finishesJunglePunAndConsumesRecorder() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("junglePuns", 11)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.JUNGLE_PUN, "step2")
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            AirportRadioChoiceSync.apply(
                html = "The tape recorder self-destructs with a shower of sparks and a puff of smoke.",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.JUNGLE_PUN))
        assertEquals(0, prefs.getInt("junglePuns"))
        assertTrue(consumed.contains(AirportRadioChoiceSync.MINI_CASSETTE_RECORDER to 1))
    }

    @Test
    fun radio_serumStepDependsOnInventory() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        AirportRadioChoiceSync.apply(
            html = "You wonder how many vials they want.",
            questDatabase = db,
            preferences = prefs,
            itemCount = { 2 },
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.SERUM))
        AirportRadioChoiceSync.apply(
            html = "You wonder how many vials they want.",
            questDatabase = db,
            preferences = prefs,
            itemCount = { 5 },
        )
        assertEquals("step1", db.getProgress(Quest.SERUM))
    }

    @Test
    fun radio_finishesOutOfOrderAndConsumesWatch() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            AirportRadioChoiceSync.apply(
                html = "He takes your nifty new watch and files it away.",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.OUT_OF_ORDER))
        assertTrue(consumed.contains(AirportRadioChoiceSync.GPS_WATCH to 1))
        assertTrue(consumed.contains(AirportRadioChoiceSync.PROJECT_TLB to 1))
    }

    @Test
    fun questChoiceRules_wiresChoice984() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = AirportRadioChoiceSync.CHOICE_ID,
                responseText = "You acquire cigarettes from the drop.",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.SMOKES))
    }
}
