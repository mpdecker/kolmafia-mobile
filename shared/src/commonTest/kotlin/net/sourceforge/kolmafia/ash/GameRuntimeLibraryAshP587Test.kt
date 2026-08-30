package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.SpacegateAdventureSync

class GameRuntimeLibraryAshP587Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun portableGate_fillsHazardsFromAdventureText() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SpacegateAdventureSync.applyFromAdventure(
                url = "adventure.php?snarfblat=494",
                html = "Toxic environment and Intense winds buffet you.",
                preferences = prefs,
            ),
        )
        assertEquals("toxic atmosphere|high winds", prefs.getString("_spacegateHazards", ""))
        assertEquals("filter helmet|high-friction boots", prefs.getString("_spacegateGear", ""))
    }

    @Test
    fun existingHazards_notOverwritten() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_spacegateHazards", "irradiated")
        prefs.setString("_spacegateGear", "rad cloak")
        assertFalse(
            SpacegateAdventureSync.applyFromAdventure(
                url = "adventure.php?snarfblat=494",
                html = "Toxic environment",
                preferences = prefs,
            ),
        )
        assertEquals("irradiated", prefs.getString("_spacegateHazards", ""))
        assertEquals("rad cloak", prefs.getString("_spacegateGear", ""))
    }
}
