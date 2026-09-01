package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightLostSync
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP624Test {

    @Test
    fun revision_phase629() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun ns3Loss_setsFinalStep12() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, "step11")
        assertTrue(QuestFightLostSync.apply("Naughty Sorceress (3)", "", db, prefs))
        assertEquals("step12", db.getProgress(Quest.FINAL))
    }

    @Test
    fun cyrusLoss_setsPrimordialAndAdjective() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = "As you black out, you remember him getting stronger."
        assertTrue(QuestFightLostSync.apply("Cyrus the Virus", html, db, prefs))
        assertEquals("step2", db.getProgress(Quest.PRIMORDIAL))
        assertEquals("stronger", prefs.getString("cyrusAdjectives"))
    }

    @Test
    fun cyrusLoss_skipsDuplicateAdjective() {
        val prefs = Preferences(MapSettings())
        prefs.setString("cyrusAdjectives", "stronger")
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightLostSync.apply(
                "Cyrus the Virus",
                "you remember him getting stronger.",
                db,
                prefs,
            ),
        )
        assertEquals("stronger", prefs.getString("cyrusAdjectives"))
    }

    @Test
    fun motherHellseal_decrementsScreeches() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_sealScreeches", 2)
        val db = QuestDatabase(prefs)
        assertTrue(QuestFightLostSync.apply("mother hellseal", "", db, prefs))
        assertEquals(1, prefs.getInt("_sealScreeches"))
        assertTrue(QuestFightLostSync.apply("mother hellseal", "", db, prefs))
        assertEquals(0, prefs.getInt("_sealScreeches"))
        assertTrue(QuestFightLostSync.apply("mother hellseal", "", db, prefs))
        assertEquals(0, prefs.getInt("_sealScreeches"))
    }

    @Test
    fun travoltron_clearsDiscoVisit() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_infernoDiscoVisited", true)
        val db = QuestDatabase(prefs)
        assertTrue(QuestFightLostSync.apply("Travoltron", "", db, prefs))
        assertFalse(prefs.getBoolean("_infernoDiscoVisited"))
    }

    @Test
    fun sourceAgentLoss_decrementsDefeated() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("sourceAgentsDefeated", 4)
        val db = QuestDatabase(prefs)
        assertTrue(QuestFightLostSync.apply("Source Agent", "", db, prefs))
        assertEquals(3, prefs.getInt("sourceAgentsDefeated"))
    }

    @Test
    fun applyCombatLoss_wiresNs3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        QuestFightRules.applyCombat(
            db, "Naughty Sorceress (3)", won = false, preferences = prefs,
        )
        assertEquals("step12", db.getProgress(Quest.FINAL))
    }
}
