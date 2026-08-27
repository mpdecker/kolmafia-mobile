package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MotorbikeChoiceSync

class GameRuntimeLibraryAshP808Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesParts() {
        val prefs = Preferences(MapSettings())
        val html = """
            <b>Tires:</b> Racing Slicks (
            <b>Gas Tank:</b> Large Capacity Tank (
            <b>Headlight:</b> Blacklight Bulb (
            <b>Cowling:</b> Armor-Plated Cowling (
            <b>Muffler:</b> Extra-Loud Muffler (
            <b>Seat:</b> Deep Seat (
        """.trimIndent()
        assertTrue(
            MotorbikeChoiceSync.applyVisit(
                choiceId = 871,
                html = html,
                preferences = prefs,
            ),
        )
        assertEquals("Racing Slicks", prefs.getString("peteMotorbikeTires", ""))
        assertEquals("Large Capacity Tank", prefs.getString("peteMotorbikeGasTank", ""))
        assertEquals("Blacklight Bulb", prefs.getString("peteMotorbikeHeadlight", ""))
        assertEquals("Armor-Plated Cowling", prefs.getString("peteMotorbikeCowling", ""))
        assertEquals("Extra-Loud Muffler", prefs.getString("peteMotorbikeMuffler", ""))
        assertEquals("Deep Seat", prefs.getString("peteMotorbikeSeat", ""))
    }
}
