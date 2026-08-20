package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MobiusChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP741Test {

    @Test
    fun visit_incrementsEncounters() {
        val prefs = Preferences(MapSettings())
        assertTrue(MobiusChoiceSync.applyVisit(1562, prefs, turnsPlayed = 50))
        assertEquals(50, prefs.getInt("_lastMobiusStripTurn", 0))
        assertEquals(1, prefs.getInt("_mobiusStripEncounters", 0))
    }

    @Test
    fun post_stockCertificate() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            MobiusChoiceSync.apply(
                choiceId = 1562,
                html = "You find a stock certificate",
                preferences = prefs,
                turnsPlayed = 77,
            ),
        )
        assertEquals(77, prefs.getInt("stockCertificateTurn", 0))
        assertEquals("77", prefs.getString("stockCertificateTurns", ""))
    }

    @Test
    fun post_timelineRepair() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("tryToRememberCharges", 1)
        assertTrue(
            MobiusChoiceSync.apply(
                choiceId = 1562,
                html = "In an effort to repair the timeline you try hard.",
                preferences = prefs,
                turnsPlayed = 10,
            ),
        )
        assertEquals(4, prefs.getInt("tryToRememberCharges", 0))
    }

    @Test
    fun questChoiceRules_wires1562() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1562,
                responseText = "You find a stock certificate",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                turnsPlayed = 12,
            ),
        )
        assertEquals(12, prefs.getInt("stockCertificateTurn", 0))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(MobiusChoiceSync.applyVisit(1219, prefs, 1))
    }
}
