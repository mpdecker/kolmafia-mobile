package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP635Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun eve_setsStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(AirportCombatSync.apply("E.V.E., the robot zombie", "", db, prefs))
        assertEquals("step1", db.getProgress(Quest.EVE))
    }

    @Test
    fun nastyBear_setsStep1UntilEight() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(AirportCombatSync.apply("nasty bear", "", db, prefs))
        assertEquals(1, prefs.getInt("dinseyNastyBearsDefeated"))
        assertEquals("step1", db.getProgress(Quest.NASTY_BEARS))
    }

    @Test
    fun nastyBear_finishesAtEightAndCaps() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dinseyNastyBearsDefeated", 7)
        val db = QuestDatabase(prefs)
        assertTrue(AirportCombatSync.apply("nasty bear", "", db, prefs))
        assertEquals(8, prefs.getInt("dinseyNastyBearsDefeated"))
        assertEquals("step2", db.getProgress(Quest.NASTY_BEARS))
        assertTrue(AirportCombatSync.apply("nasty bear", "", db, prefs))
        assertEquals(8, prefs.getInt("dinseyNastyBearsDefeated"))
        assertEquals("step2", db.getProgress(Quest.NASTY_BEARS))
    }

    @Test
    fun applyCombatWin_wiresEve() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db, "E.V.E., the robot zombie", won = true, preferences = prefs,
            ).advanced,
        )
        assertEquals("step1", db.getProgress(Quest.EVE))
    }
}
