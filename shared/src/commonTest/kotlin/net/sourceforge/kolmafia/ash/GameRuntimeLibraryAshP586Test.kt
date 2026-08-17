package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.SpacegateTerminalSync

class GameRuntimeLibraryAshP586Test {

    private val FIXTURE = """
        <html><body>
        Spacegate Terminal
        <td>Current planet: Planet Name: Alpha Centauri<br>
        <br>Coordinates: B12<br>
        <br><p>Environmental Hazards:<br>toxic atmosphere<br>high gravity<br>Plant Life: lush<br>
        <br>Plant Life: lush<br>
        <br>Animal Life: abundant <font color=red>(hostile)</font><br>
        <br>Intelligent Life: none<br>
        <b>Spant</b>
        <br>ALERT: ANCIENT RUINS DETECTED<br>
        <p>Spacegate Energy remaining: <b><font size=+2>11 </font>
        </body></html>
    """.trimIndent()

    @Test
    fun terminal_parsesPlanetCoordsHazards() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SpacegateTerminalSync.applyFromTerminal(
                url = "place.php?whichplace=spacegate&action=sg_Terminal",
                html = FIXTURE,
                preferences = prefs,
            ),
        )
        assertEquals("Alpha Centauri", prefs.getString("_spacegatePlanetName", ""))
        assertEquals("B12", prefs.getString("_spacegateCoordinates", ""))
        assertEquals(1, prefs.getInt("_spacegatePlanetIndex", -1))
        assertTrue(prefs.getString("_spacegateHazards", "").contains("toxic atmosphere"))
        assertTrue(prefs.getString("_spacegateGear", "").contains("filter helmet"))
        assertTrue(prefs.getString("_spacegateGear", "").contains("exo-servo leg braces"))
    }

    @Test
    fun terminal_parsesLifeFlagsAndTurns() {
        val prefs = Preferences(MapSettings())
        SpacegateTerminalSync.applyFromTerminal(
            url = "place.php?whichplace=spacegate&action=sg_Terminal",
            html = FIXTURE,
            preferences = prefs,
        )
        assertEquals("lush", prefs.getString("_spacegatePlantLife", ""))
        assertTrue(prefs.getString("_spacegateAnimalLife", "").startsWith("abundant"))
        assertEquals("none", prefs.getString("_spacegateIntelligentLife", ""))
        assertTrue(prefs.getBoolean("_spacegateSpant", false))
        assertTrue(prefs.getBoolean("_spacegateRuins", false))
        assertEquals("11", prefs.getString("_spacegateTurnsLeft", ""))
    }
}
