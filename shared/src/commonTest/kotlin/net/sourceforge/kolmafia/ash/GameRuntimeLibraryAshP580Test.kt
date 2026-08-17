package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.HiddenCityChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP580Test {

    @Test
    fun elevator_emptyPenthouse_setsProgress7() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("hiddenApartmentProgress", 6)
        assertTrue(
            HiddenCityChoiceSync.applyPostChoice(
                choiceId = 780,
                html = "The penthouse is empty now",
                decision = 1,
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(7, prefs.getInt("hiddenApartmentProgress", 0))
    }

    @Test
    fun shrine_start_setsCursesStarted() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityChoiceSync.applyPostChoice(
                choiceId = 781,
                html = "Earthbound and Down",
                decision = 1,
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt("hiddenApartmentProgress", 0))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.CURSES))
    }

    @Test
    fun bowling_incrementsFrom2To3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("hiddenBowlingAlleyProgress", 2)
        assertTrue(
            HiddenCityChoiceSync.applyPostChoice(
                choiceId = 788,
                html = "You bowl a frame",
                decision = 1,
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(3, prefs.getInt("hiddenBowlingAlleyProgress", 0))
    }

    @Test
    fun park_choice_relocatesJanitor() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityChoiceSync.applyPostChoice(
                choiceId = 789,
                html = "Where Does The Lone Ranger Take His Garbagester?",
                decision = 2,
                questDatabase = db,
                preferences = prefs,
                ascensionNumber = 9,
            ),
        )
        assertEquals(9, prefs.getInt("relocatePygmyJanitor", -1))
    }

    @Test
    fun visit791_setsZigguratLianas() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            HiddenCityChoiceSync.applyVisitChoice(791, "Legend of the Temple", prefs),
        )
        assertEquals(1, prefs.getInt("zigguratLianas", 0))
    }
}
