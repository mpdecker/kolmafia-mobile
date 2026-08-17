package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FarmDuckSync

class GameRuntimeLibraryAshP592Test {

    @Test
    fun firstClear_setsCsv() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            FarmDuckSync.applyFromAdventure(
                "141",
                "There are no more ducks here",
                prefs,
            ),
        )
        assertEquals("141", prefs.getString("duckAreasCleared", ""))
    }

    @Test
    fun secondClear_appends() {
        val prefs = Preferences(MapSettings())
        prefs.setString("duckAreasCleared", "141")
        assertTrue(
            FarmDuckSync.applyFromAdventure(
                "142",
                "There are no more ducks here",
                prefs,
            ),
        )
        assertEquals("141,142", prefs.getString("duckAreasCleared", ""))
    }

    @Test
    fun duplicate_notAppended() {
        val prefs = Preferences(MapSettings())
        prefs.setString("duckAreasCleared", "141,142")
        assertFalse(
            FarmDuckSync.applyFromAdventure(
                "141",
                "There are no more ducks here",
                prefs,
            ),
        )
        assertEquals("141,142", prefs.getString("duckAreasCleared", ""))
    }
}
