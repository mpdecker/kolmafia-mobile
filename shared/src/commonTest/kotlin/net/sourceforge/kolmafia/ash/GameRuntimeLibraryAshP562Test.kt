package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.MayamAvailability
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP562Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun mayam_bare_listsAvailableResonances() {
        val prefs = Preferences(MapSettings())
        // empty symbols-used → all resonances available
        val expected = MayamAvailability.availableResonances(prefs)
        assertTrue(expected.isNotEmpty())
        val out = outputLib(
            GameRuntimeLibrary(preferences = prefs),
            """cli_execute("mayam");""",
        )
        assertTrue(out.contains(expected.first(), ignoreCase = true))
    }

    @Test
    fun mayam_list_sameAsBare() {
        val prefs = Preferences(MapSettings())
        val bare = outputLib(GameRuntimeLibrary(preferences = prefs), """cli_execute("mayam");""")
        val list = outputLib(GameRuntimeLibrary(preferences = prefs), """cli_execute("mayam list");""")
        assertEquals(bare, list)
    }

    @Test
    fun help_listsMayam() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help mayam");""")
        assertTrue(out.lines().any { it.trim() == "mayam" })
    }
}
