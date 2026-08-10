package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP305Test {

    @AfterTest
    fun tearDown() {
        // no shared singleton state
    }

    @Test
    fun revision_isphase312() {
        assertEquals("phase390", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun get_fishing_locations_returnsMapFromPrefs() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_floundryCarpLocation", "The Distant Woods")
        prefs.setString("_floundryCodLocation", "The Sea")
        val lib = GameRuntimeLibrary(preferences = prefs)
        val output = outputLib(
            lib,
            """
            foreach key, loc in get_fishing_locations() {
              print(key + ":" + loc);
            }
            """.trimIndent(),
        ).trim()
        assertEquals("carp:The Distant Woods\ncod:The Sea", output)
    }
}
