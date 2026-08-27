package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DetectiveCaseSync

class GameRuntimeLibraryAshP621Test {

    @Test
    fun revision_phase623() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun solvedCase_increments() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DetectiveCaseSync.applyFromVisit(
                url = "wham.php",
                html = "Congratulations! You solved the case.",
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt("_detectiveCasesCompleted"))
        assertTrue(
            DetectiveCaseSync.applyFromVisit(
                url = "wham.php?action=solve",
                html = "Congratulations! You solved the case of the missing pie.",
                preferences = prefs,
            ),
        )
        assertEquals(2, prefs.getInt("_detectiveCasesCompleted"))
    }

    @Test
    fun otherPage_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            DetectiveCaseSync.applyFromVisit(
                url = "council.php",
                html = "Congratulations! You solved the case",
                preferences = prefs,
            ),
        )
        assertEquals(0, prefs.getInt("_detectiveCasesCompleted", 0))
    }
}
