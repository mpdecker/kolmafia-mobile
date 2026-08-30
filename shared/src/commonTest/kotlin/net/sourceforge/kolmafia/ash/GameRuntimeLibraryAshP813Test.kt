package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DetectiveCaseSync

class GameRuntimeLibraryAshP813Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesCasesRemaining() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DetectiveCaseSync.applyVisit(
                choiceId = 1193,
                html = "You have (1 more case) to solve",
                preferences = prefs,
            ),
        )
        assertEquals(2, prefs.getInt("_detectiveCasesCompleted", 0))
    }

    @Test
    fun wham_incrementStillWorks() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_detectiveCasesCompleted", 1)
        assertTrue(
            DetectiveCaseSync.applyFromVisit(
                url = "wham.php",
                html = "Congratulations! You solved the case",
                preferences = prefs,
            ),
        )
        assertEquals(2, prefs.getInt("_detectiveCasesCompleted", 0))
    }
}
