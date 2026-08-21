package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BugbearChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.BugbearManager

class GameRuntimeLibraryAshP702Test {

    @Test
    fun revision_phase707() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun sonar_clearsZoneAndOpensNextLevel() {
        val prefs = Preferences(MapSettings())
        prefs.setString("statusMedbay", "cleared")
        prefs.setString("statusWasteProcessing", "cleared")
        prefs.setString("statusSonar", "open")
        prefs.setString("statusScienceLab", "unlocked")
        prefs.setInt("mothershipProgress", 0)
        assertTrue(
            BugbearChoiceSync.apply(
                588,
                "The batbugbears around you start acting weird",
                prefs,
            ),
        )
        assertEquals("cleared", prefs.getString("statusSonar"))
        assertEquals(1, prefs.getInt("mothershipProgress"))
        assertEquals("open", prefs.getString("statusScienceLab"))
    }

    @Test
    fun clearShipZone_skipsAlreadyCleared() {
        val prefs = Preferences(MapSettings())
        prefs.setString("statusSonar", "cleared")
        prefs.setInt("mothershipProgress", 0)
        BugbearManager.clearShipZone("Sonar", prefs)
        assertEquals(0, prefs.getInt("mothershipProgress"))
    }

    @Test
    fun sonar_requiresPhrase() {
        val prefs = Preferences(MapSettings())
        assertFalse(BugbearChoiceSync.apply(588, "nothing happens", prefs))
    }

    @Test
    fun questChoiceRules_wires588() {
        val prefs = Preferences(MapSettings())
        prefs.setString("statusSonar", "open")
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 588,
                responseText = "The batbugbears around you start acting weird",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("cleared", prefs.getString("statusSonar"))
    }
}
