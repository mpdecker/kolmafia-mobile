package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP630Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun burger_incrementsWithoutFinishing() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("buffJimmyIngredients", 3)
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportCombatSync.apply(
                "Sloppy Seconds Burger",
                "You consult the list and grab the next ingredient",
                db,
                prefs,
            ),
        )
        assertEquals(4, prefs.getInt("buffJimmyIngredients"))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.JIMMY_CHEESEBURGER))
    }

    @Test
    fun burger_finishesAtFifteen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("buffJimmyIngredients", 14)
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportCombatSync.apply(
                "Sloppy Seconds Burger",
                "You consult the list and grab the next ingredient",
                db,
                prefs,
            ),
        )
        assertEquals(15, prefs.getInt("buffJimmyIngredients"))
        assertEquals("step1", db.getProgress(Quest.JIMMY_CHEESEBURGER))
    }

    @Test
    fun burger_withoutListIsNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            AirportCombatSync.apply("Sloppy Seconds Burger", "You win the fight", db, prefs),
        )
        assertEquals(0, prefs.getInt("buffJimmyIngredients", 0))
    }

    @Test
    fun applyCombatWin_wiresBurger() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("buffJimmyIngredients", 14)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                "Sloppy Seconds Burger",
                won = true,
                preferences = prefs,
                responseText = "You consult the list and grab the next ingredient",
            ).advanced,
        )
        assertEquals("step1", db.getProgress(Quest.JIMMY_CHEESEBURGER))
    }
}
