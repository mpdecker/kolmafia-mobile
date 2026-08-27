package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FavoriteBirdChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP798Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_setsBirdsSought() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            FavoriteBirdChoiceSync.apply(
                choiceId = 1399,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(6, prefs.getInt("_birdsSoughtToday", 0))
        assertEquals("x", prefs.getString("yourFavoriteBird", "x"))
    }

    @Test
    fun post_decision1_copiesBirdAndLearns() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_birdOfTheDay", "Magpie")
        val learned = mutableListOf<Int>()
        assertTrue(
            FavoriteBirdChoiceSync.apply(
                choiceId = 1399,
                decision = 1,
                preferences = prefs,
                learnSkill = { learned += it },
            ),
        )
        assertEquals("Magpie", prefs.getString("yourFavoriteBird", ""))
        assertEquals(listOf(FavoriteBirdChoiceSync.VISIT_FAVORITE_BIRD_SKILL_ID), learned)
    }

    @Test
    fun questChoiceRules_wires1399() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_birdOfTheDay", "Crow")
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1399,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(6, prefs.getInt("_birdsSoughtToday", 0))
        assertEquals("Crow", prefs.getString("yourFavoriteBird", ""))
        assertEquals(1, prefs.getInt("skillLevel190", 0))
    }
}
