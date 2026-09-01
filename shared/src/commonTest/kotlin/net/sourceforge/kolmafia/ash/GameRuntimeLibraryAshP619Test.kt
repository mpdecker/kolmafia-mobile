package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.TavernCellarSync

class GameRuntimeLibraryAshP619Test {

    @Test
    fun revision_phase623() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cellarMap_buildsLayout() {
        val prefs = Preferences(MapSettings())
        val html = """
            <img alt="Stairs Up (1,1)">
            <img alt="Explored (2,1)">
            <img alt="Darkness (3,1)">
            <img alt="A Rat Faucet (4,1)">
            <img alt="A Tiny Mansion (5,1)">
        """.trimIndent()
        assertTrue(TavernCellarSync.parseCellarMap(html, prefs, ascensionNumber = 1))
        val layout = prefs.getString("tavernLayout")
        assertEquals(25, layout.length)
        assertEquals('1', layout[0])
        assertEquals('1', layout[1])
        assertEquals('0', layout[2])
        assertEquals('3', layout[3])
        assertEquals('4', layout[4])
    }

    @Test
    fun defeatedMansion_usesCode6() {
        val prefs = Preferences(MapSettings())
        val html = """<img alt="A Tiny Mansion (1,1)" src="mansion2.gif">"""
        assertTrue(TavernCellarSync.parseCellarMap(html, prefs, ascensionNumber = 1))
        assertEquals('6', prefs.getString("tavernLayout")[0])
    }

    @Test
    fun exploredDoesNotOverwriteBarrel() {
        val prefs = Preferences(MapSettings())
        prefs.setString("tavernLayout", "5" + "0".repeat(24))
        val html = """<img alt="Explored (1,1)">"""
        assertFalse(TavernCellarSync.parseCellarMap(html, prefs, ascensionNumber = 0))
        assertEquals('5', prefs.getString("tavernLayout")[0])
    }

    @Test
    fun ascensionReset_clearsLayout() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastTavernAscension", 0)
        prefs.setString("tavernLayout", "1".repeat(25))
        prefs.setInt("lastTavernSquare", 7)
        TavernCellarSync.validateFaucetQuest(prefs, ascensionNumber = 2)
        assertEquals(TavernCellarSync.EMPTY_LAYOUT, prefs.getString("tavernLayout"))
        assertEquals(0, prefs.getInt("lastTavernSquare"))
        assertEquals(2, prefs.getInt("lastTavernAscension"))
    }
}
