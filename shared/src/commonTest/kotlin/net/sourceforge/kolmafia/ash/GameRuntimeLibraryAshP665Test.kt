package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportNpcChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP665Test {

    @Test
    fun revision_phase665() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun jimmy_startsAndFinishesMushroomQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            AirportNpcChoiceSync.apply(
                choiceId = AirportNpcChoiceSync.JIMMY_CHOICE,
                html = "Those skinny mushroom girls keep stealing my stash.",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.JIMMY_MUSHROOM))
        assertTrue(
            AirportNpcChoiceSync.apply(
                choiceId = AirportNpcChoiceSync.JIMMY_CHOICE,
                html = "But here's a few Beach Bucks as a token of my changes in gratitude.",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.JIMMY_MUSHROOM))
        assertTrue(consumed.contains(AirportNpcChoiceSync.PENCIL_THIN_MUSHROOM to 10))
    }

    @Test
    fun tacoDan_finishesAuditAndConsumesReceipts() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            AirportNpcChoiceSync.apply(
                choiceId = AirportNpcChoiceSync.TACO_DAN_CHOICE,
                html = "Here's a little Taco Dan's Taco Stand gratitude for ya.",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.TACO_DAN_AUDIT))
        assertTrue(consumed.contains(AirportNpcChoiceSync.TACO_DAN_RECEIPT to 10))
    }

    @Test
    fun broden_startsSprinklesAndConsumesShakerOnFinish() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            AirportNpcChoiceSync.apply(
                choiceId = AirportNpcChoiceSync.BRODEN_CHOICE,
                html = "I'll loan you my sprinkle shaker to fill up.",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.BRODEN_SPRINKLES))
        assertEquals(0, prefs.getInt("brodenSprinkles"))
        assertTrue(
            AirportNpcChoiceSync.apply(
                choiceId = AirportNpcChoiceSync.BRODEN_CHOICE,
                html = "Now I can sell some <i>deluxe</i> brogurts.",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.BRODEN_SPRINKLES))
        assertTrue(consumed.contains(AirportNpcChoiceSync.SPRINKLE_SHAKER to 1))
    }

    @Test
    fun jimmySalt_consumesFiftySalt() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        AirportNpcChoiceSync.apply(
            choiceId = AirportNpcChoiceSync.JIMMY_CHOICE,
            html = "So here's some Beach Bucks instead.",
            questDatabase = db,
            preferences = prefs,
            consumeItem = { id, qty -> consumed.add(id to qty) },
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.JIMMY_SALT))
        assertTrue(consumed.contains(AirportNpcChoiceSync.SAILOR_SALT to 50))
    }

    @Test
    fun questChoiceRules_wiresChoice915() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = AirportNpcChoiceSync.JIMMY_CHOICE,
                responseText = "I'm not really into moving out of this hammock.",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.JIMMY_CHEESEBURGER))
        assertEquals(0, prefs.getInt("buffJimmyIngredients"))
    }

    @Test
    fun brodenDebt_consumesFifteenBroupons() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            AirportNpcChoiceSync.apply(
                choiceId = AirportNpcChoiceSync.BRODEN_CHOICE,
                html = "And they all had broupons, huh?",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.BRODEN_DEBT))
        assertTrue(consumed.contains(AirportNpcChoiceSync.BROUPON to 15))
    }
}
