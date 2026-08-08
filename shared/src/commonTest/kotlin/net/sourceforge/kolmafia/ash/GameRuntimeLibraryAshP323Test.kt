package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP323Test {

    @Test
    fun visitHook_fightPhp_parsesKillAndHealScrollClues() {
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.processVisitResponseHooks(
            html = """
                You're fighting a mer-kin.
                a magnificent <b>planetfish</b> smiling
                recognize one of them: <b>&quot;black&quot;</b>
            """.trimIndent(),
            url = "https://www.kingdomofloathing.com/fight.php",
        )
        assertEquals(4, prefs.getInt("dreadScroll2", 0))
        assertEquals(2, prefs.getInt("dreadScroll5", 0))
    }

    @Test
    fun visitHook_choice704_parsesLibraryClue() {
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.processVisitResponseHooks(
            html = "somebody has scrawled &quot;<b>FOURTH</b>&quot;",
            url = "https://www.kingdomofloathing.com/choice.php?whichchoice=704&option=1",
        )
        assertEquals(4, prefs.getInt("dreadScroll1", 0))
    }

    @Test
    fun cliDreadscroll_printsCluesAndScrollText() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dreadScroll1", 1)
        prefs.setInt("dreadScroll2", 1)
        val lib = GameRuntimeLibrary(preferences = prefs)
        val out = outputLib(lib, """cli_execute("dreadscroll");""")
        assertTrue(out.contains("dreadScroll1 (Mer-kin Library 1): 1 (LONELY)"))
        assertTrue(out.contains("the Elder shall awaken."))
    }
}
