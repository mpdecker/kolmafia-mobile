package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FloristFriarChoiceSync
import net.sourceforge.kolmafia.request.ChateauRequest
import net.sourceforge.kolmafia.request.FloristRequest
import net.sourceforge.kolmafia.session.LocketManager

class GameRuntimeLibraryPhase3770Test {

    @AfterTest
    fun tearDown() {
        LocketManager.clear()
        FloristRequest.reset()
        ChateauRequest.reset()
    }

    @Test
    fun revision_phase3770() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun getFloristPlants_fromCatalog() {
        FloristFriarChoiceSync.reset()
        FloristFriarChoiceSync.apply(
            720,
            "choice.php?whichchoice=720&option=1&plant=7",
            "The Florist Friar's Cottage Ah, <b>The Sleazy Back Alley</b>!",
            Preferences(MapSettings()),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("1", outputLib(lib, "print(count(get_florist_plants()));"))
    }

    @Test
    fun getChateau_fromFurnitureGifs() {
        ChateauRequest.parseFurniture("<img src=nightstand_moxie.gif><img src=chandelier.gif>")
        val lib = GameRuntimeLibrary()
        assertEquals("2", outputLib(lib, "print(count(get_chateau()));"))
    }

    @Test
    fun getLocketMonsters_catalogNotFought() {
        LocketManager.clear()
        LocketManager.rememberMonster(101)
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertEquals("1", outputLib(lib, "print(count(get_locket_monsters()));"))
    }

    @Test
    fun gitAndSvnStubs() {
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(git_exists("foo"));"""))
        assertEquals("false", outputLib(lib, """print(svn_at_head("foo"));"""))
        assertEquals("0", outputLib(lib, "print(count(git_list()));"))
        assertEquals("0", outputLib(lib, "print(count(svn_list()));"))
        assertEquals("", outputLib(lib, """print(git_info("foo").url);"""))
        assertEquals("0", outputLib(lib, """print(svn_info("foo").revision);"""))
    }

    @Test
    fun floristAvailable_usesFriarPrefs() {
        val lib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings().apply {
                putBoolean("floristFriarChecked", true)
                putBoolean("floristFriarAvailable", true)
            }),
        )
        assertEquals("true", outputLib(lib, "print(florist_available());"))
    }

    @Test
    fun pingpongCli_requiresPlayer() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("pingpong");""")
        assertTrue(out.contains("Play ping-pong with whom?"), out)
    }

    @Test
    fun pingpongDoesNotDispatchAsPing() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("pingpong");""")
        assertTrue(!out.contains("Usage: ping", ignoreCase = true), out)
    }
}
