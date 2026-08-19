package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ClancyNcSync
import net.sourceforge.kolmafia.quest.IslandWarVisitSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP671Test {

    @Test
    fun revision_phase671() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun clancy_minstrelChoicesWriteEvenSteps() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(ClancyNcSync.applyFromChoice(571, db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.CLANCY))
        assertTrue(ClancyNcSync.applyFromChoice(572, db))
        assertEquals("step2", db.getProgress(Quest.CLANCY))
        assertTrue(ClancyNcSync.applyFromChoice(577, db))
        assertEquals("step8", db.getProgress(Quest.CLANCY))
    }

    @Test
    fun islandWar_enlistDecision3StartsWar() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(IslandWarVisitSync.applyFromEnlistChoice(3, db, prefs))
        assertEquals("step1", db.getProgress(Quest.ISLAND_WAR))
        assertEquals("started", prefs.getString("warProgress"))
    }

    @Test
    fun islandWar_pokefamSetsDefeatedCounts() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        IslandWarVisitSync.applyFromEnlistChoice(3, db, prefs, inPokefam = true)
        assertEquals(500, prefs.getInt("hippiesDefeated"))
        assertEquals(500, prefs.getInt("fratboysDefeated"))
    }

    @Test
    fun manor921_setsStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 921,
                responseText = "We'll All Be Flat",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.MANOR))
    }

    @Test
    fun questChoiceRules_wiresClancy572() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 572,
                responseText = "Your Minstrel Clamps",
                questDatabase = db,
            ),
        )
        assertEquals("step2", db.getProgress(Quest.CLANCY))
    }

    @Test
    fun questChoiceRules_wiresIslandWar142() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 142,
                responseText = "enlist",
                questDatabase = db,
                decision = 3,
                preferences = prefs,
            ),
        )
        assertEquals("started", prefs.getString("warProgress"))
    }
}
