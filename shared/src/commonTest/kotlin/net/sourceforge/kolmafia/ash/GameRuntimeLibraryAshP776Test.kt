package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AprilBandChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.AprilBandRequest

class GameRuntimeLibraryAshP776Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun choiceId_is1526NotPlaque() {
        assertEquals(1526, AprilBandRequest.CHOICE_ID)
        assertEquals(1526, AprilBandChoiceSync.CHOICE_ID)
    }

    @Test
    fun conduct_setsNextTurn() {
        val prefs = Preferences(MapSettings())
        assertTrue(AprilBandChoiceSync.apply(1526, 2, prefs, turnsPlayed = 100))
        assertEquals(111, prefs.getInt("nextAprilBandTurn", 0))
    }

    @Test
    fun instrument_incrementsCapped() {
        val prefs = Preferences(MapSettings())
        assertTrue(AprilBandChoiceSync.apply(1526, 4, prefs))
        assertTrue(AprilBandChoiceSync.apply(1526, 5, prefs))
        assertTrue(AprilBandChoiceSync.apply(1526, 6, prefs))
        assertEquals(2, prefs.getInt("_aprilBandInstruments", 0))
    }

    @Test
    fun questChoiceRules_wires1526() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1526,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                turnsPlayed = 50,
            ),
        )
        assertEquals(61, prefs.getInt("nextAprilBandTurn", 0))
    }
}
