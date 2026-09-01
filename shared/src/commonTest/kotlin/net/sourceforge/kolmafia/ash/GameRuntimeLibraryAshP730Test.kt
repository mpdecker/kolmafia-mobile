package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SleazeAirportExtendedChoiceSync

class GameRuntimeLibraryAshP730Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun yachtzee_consumesBeads() {
        var consumedId = -1
        var consumedQty = 0
        assertTrue(
            SleazeAirportExtendedChoiceSync.apply(
                choiceId = 918,
                decision = 3,
                html = "You open the captain's door",
                questDatabase = null,
                preferences = null,
                itemCount = { 42 },
                consumeItem = { id, qty ->
                    consumedId = id
                    consumedQty = qty
                },
            ),
        )
        assertEquals(SleazeAirportExtendedChoiceSync.MOIST_BEADS, consumedId)
        assertEquals(42, consumedQty)
    }

    @Test
    fun breakTime_incrementsBucks() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SleazeAirportExtendedChoiceSync.apply(
                choiceId = 919,
                decision = 1,
                html = "You find some Beach Bucks",
                questDatabase = null,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt("_sloppyDinerBeachBucks", 0))
        assertTrue(
            SleazeAirportExtendedChoiceSync.apply(
                choiceId = 919,
                decision = 1,
                html = "You've already thoroughly searched",
                questDatabase = null,
                preferences = prefs,
            ),
        )
        assertEquals(4, prefs.getInt("_sloppyDinerBeachBucks", 0))
    }

    @Test
    fun eraser_resetsJimmyQuests() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.STARTED)
        db.setProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.FINISHED)
        var consumedId = -1
        assertTrue(
            SleazeAirportExtendedChoiceSync.apply(
                choiceId = 920,
                decision = 1,
                html = "",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, _ -> consumedId = id },
            ),
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.JIMMY_MUSHROOM))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.JIMMY_CHEESEBURGER))
        assertEquals(SleazeAirportExtendedChoiceSync.MIND_DESTROYER, consumedId)
    }

    @Test
    fun questChoiceRules_wires920() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.STARTED)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 920,
                responseText = "",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.TACO_DAN_AUDIT))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(
            SleazeAirportExtendedChoiceSync.apply(
                choiceId = 915,
                decision = 1,
                html = "You open the captain's door",
                questDatabase = null,
                preferences = null,
            ),
        )
    }
}
