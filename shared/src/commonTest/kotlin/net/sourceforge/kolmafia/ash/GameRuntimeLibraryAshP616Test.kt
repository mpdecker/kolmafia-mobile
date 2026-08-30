package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.IslandUnlockSync

class GameRuntimeLibraryAshP616Test {

    @Test
    fun revision_phase616() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun main_withIslandLink_setsPref() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            IslandUnlockSync.applyFromMain(
                url = "main.php",
                html = """<a href="island.php">The Mysterious Island</a>""",
                preferences = prefs,
                ascensionNumber = 7,
            ),
        )
        assertEquals(7, prefs.getInt("lastIslandUnlock", -1))
    }

    @Test
    fun alreadyThisAscension_isNoOp() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastIslandUnlock", 7)
        assertFalse(
            IslandUnlockSync.applyFromMain(
                url = "main.php",
                html = "island.php",
                preferences = prefs,
                ascensionNumber = 7,
            ),
        )
        assertEquals(7, prefs.getInt("lastIslandUnlock", -1))
    }

    @Test
    fun withoutIslandLink_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            IslandUnlockSync.applyFromMain(
                url = "main.php",
                html = "Welcome to the Kingdom",
                preferences = prefs,
                ascensionNumber = 3,
            ),
        )
        assertEquals(-1, prefs.getInt("lastIslandUnlock", -1))
    }
}
