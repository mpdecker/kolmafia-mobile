package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PalindomeSync
import net.sourceforge.kolmafia.quest.PyramidVisitSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SeaVisitSync

class GameRuntimeLibraryAshP677Test {

    @Test
    fun revision_phase677() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun palindome_517SetsStep3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PALINDOME, QuestDatabase.STARTED)
        assertTrue(PalindomeSync.applyFromChoice(db))
        assertEquals("step3", db.getProgress(Quest.PALINDOME))
    }

    @Test
    fun pyramid_132Decision2SetsStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            PyramidVisitSync.applyFromChoice(
                choiceId = PyramidVisitSync.LETS_MAKE_A_DEAL,
                decision = 2,
                html = "",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.PYRAMID))
    }

    @Test
    fun pyramid_929WheelWrapsAndConsumes() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("pyramidPosition", 5)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            PyramidVisitSync.applyFromChoice(
                choiceId = PyramidVisitSync.CONTROL_FREAK,
                decision = 1,
                html = "wooden wheel disintegrating",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(1, prefs.getInt("pyramidPosition"))
        assertTrue(consumed.contains(PyramidVisitSync.CRUMBLING_WHEEL to 1))
    }

    @Test
    fun sea_299RescuesBrotherAndRecordsHatch() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setBoolean("bigBrotherRescued", true)
        assertTrue(
            SeaVisitSync.applyFromChoice(
                choiceId = 299,
                decision = 1,
                questDatabase = db,
                preferences = prefs,
                turnsPlayed = 4321,
            ),
        )
        assertEquals(4321, prefs.getInt("_lastFitzsimmonsHatch"))
        assertEquals("step2", db.getProgress(Quest.SEA_MONKEES))
        assertTrue(prefs.getBoolean("bigBrotherRescued"))
    }

    @Test
    fun sea_302SetsStep5() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaVisitSync.applyFromChoice(
                choiceId = 302,
                decision = 1,
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("step5", db.getProgress(Quest.SEA_MONKEES))
    }

    @Test
    fun questChoiceRules_wires517And299() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PALINDOME, QuestDatabase.STARTED)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 517,
                responseText = "Mr. Alarm",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("step3", db.getProgress(Quest.PALINDOME))
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 299,
                responseText = "Down at the Hatch",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                turnsPlayed = 99,
            ),
        )
        assertEquals("step2", db.getProgress(Quest.SEA_MONKEES))
        assertTrue(prefs.getBoolean("bigBrotherRescued"))
    }
}
