package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP552Test {

    @Test
    fun revision_phase556() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun ocean_status_printsPrefs() {
        val prefs = Preferences(MapSettings())
        prefs.setString("oceanDestination", "plinth")
        prefs.setString("oceanAction", "stop")
        val out = outputLib(
            GameRuntimeLibrary(preferences = prefs),
            """cli_execute("ocean");""",
        )
        assertTrue(out.contains("plinth", ignoreCase = true))
        assertTrue(out.contains("stop", ignoreCase = true))
    }

    @Test
    fun ocean_dest_setsKeywordAndCoords() {
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        outputLib(lib, """cli_execute("ocean dest muscle");""")
        assertEquals("muscle", prefs.getString("oceanDestination", ""))
        outputLib(lib, """cli_execute("ocean destination 10,20");""")
        assertEquals("10,20", prefs.getString("oceanDestination", ""))
    }

    @Test
    fun ocean_dest_rejectsInvalidCoords() {
        val prefs = Preferences(MapSettings())
        prefs.setString("oceanDestination", "manual")
        val out = outputLib(
            GameRuntimeLibrary(preferences = prefs),
            """cli_execute("ocean dest 999,999");""",
        )
        assertTrue(out.contains("not valid", ignoreCase = true))
        assertEquals("manual", prefs.getString("oceanDestination", ""))
    }

    @Test
    fun ocean_action_acceptsSpacedAliases() {
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        outputLib(lib, """cli_execute("ocean action save and continue");""")
        assertEquals("savecontinue", prefs.getString("oceanAction", ""))
        outputLib(lib, """cli_execute("ocean action saveshow");""")
        assertEquals("saveshow", prefs.getString("oceanAction", ""))
    }

    @Test
    fun ocean_list_and_help() {
        val listOut = outputLib(GameRuntimeLibrary(preferences = Preferences(MapSettings())), """cli_execute("ocean list");""")
        assertTrue(listOut.contains("muscle", ignoreCase = true))
        assertTrue(listOut.contains("savecontinue", ignoreCase = true))
        val help = outputLib(GameRuntimeLibrary(), """cli_execute("help ocean");""")
        assertTrue(help.lines().any { it.trim() == "ocean" })
    }
}
