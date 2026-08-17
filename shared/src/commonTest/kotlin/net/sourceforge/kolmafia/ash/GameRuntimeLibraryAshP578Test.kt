package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.ShenSync

class GameRuntimeLibraryAshP578Test {

    @Test
    fun visit851_parsesQuestItem() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            ShenSync.applyVisitChoice(
                ShenSync.CHOICE_NIGHTCLUB,
                "Bring me <b>first pizza</b>, hidden away for centuries",
                prefs,
            ),
        )
        assertEquals("first pizza", prefs.getString("shenQuestItem", ""))
    }

    @Test
    fun post851_setsStep1AndInitiationDay() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ShenSync.applyPostChoice(
                choiceId = ShenSync.CHOICE_NIGHTCLUB,
                html = "artifact known only as <b>lacrosse stick</b>, hidden away for centuries",
                questDatabase = db,
                preferences = prefs,
                dayCount = 3,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.SHEN))
        assertEquals(3, prefs.getInt("shenInitiationDay", -1))
        assertEquals("lacrosse stick", prefs.getString("shenQuestItem", ""))
    }

    @Test
    fun post852_advancesAndUpdatesItem() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.SHEN, "step1")
        prefs.setString("shenQuestItem", "first pizza")
        val consumed = mutableListOf<Int>()
        assertTrue(
            ShenSync.applyPostChoice(
                choiceId = ShenSync.CHOICE_JERK,
                html = "Bring me <b>eye of the stars</b>, hidden away for centuries",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { consumed.add(it) },
            ),
        )
        assertEquals("step2", db.getProgress(Quest.SHEN))
        assertEquals("eye of the stars", prefs.getString("shenQuestItem", ""))
        assertTrue(consumed.contains(ShenSync.FIRST_PIZZA))
    }

    @Test
    fun post854_finishesAndClearsItem() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.SHEN, "step3")
        prefs.setString("shenQuestItem", "shield of brook")
        assertTrue(
            ShenSync.applyPostChoice(
                choiceId = ShenSync.CHOICE_WORLDS_BIGGEST,
                html = "Thanks",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.SHEN))
        assertEquals("", prefs.getString("shenQuestItem", "x"))
    }

    @Test
    fun charm_finishesShen() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ShenSync.applyItemAcquire(
                ShenSync.COPPERHEAD_CHARM,
                db,
                hasItemId = { it == ShenSync.COPPERHEAD_CHARM },
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.SHEN))
    }
}
