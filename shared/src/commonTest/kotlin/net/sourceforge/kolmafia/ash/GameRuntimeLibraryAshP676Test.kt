package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.ToppingPeakNcSync

class GameRuntimeLibraryAshP676Test {

    @Test
    fun revision_phase677() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun horror_beatenUpSubtractsTwo() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("booPeakProgress", 50)
        assertTrue(
            ToppingPeakNcSync.applyFromChoice(
                decision = 1,
                html = "That's all the horror you can take",
                optionLabel = "Read the Book",
                preferences = prefs,
            ),
        )
        assertEquals(48, prefs.getInt("booPeakProgress"))
    }

    @Test
    fun horror_levelFiveSubtractsTen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("booPeakProgress", 50)
        ToppingPeakNcSync.applyFromChoice(
            decision = 1,
            html = "You steel yourself and press on.",
            optionLabel = "Read the Book",
            preferences = prefs,
        )
        assertEquals(40, prefs.getInt("booPeakProgress"))
    }

    @Test
    fun horror_fleeIsNoOp() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("booPeakProgress", 50)
        assertFalse(
            ToppingPeakNcSync.applyFromChoice(
                decision = 2,
                html = "You run away.",
                optionLabel = "Flee",
                preferences = prefs,
            ),
        )
        assertEquals(50, prefs.getInt("booPeakProgress"))
    }

    @Test
    fun questChoiceRules_wires611() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("booPeakProgress", 12)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 611,
                responseText = "That's all the horror you can take",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                optionLabel = "Ask the Question",
            ),
        )
        assertEquals(10, prefs.getInt("booPeakProgress"))
    }
}
