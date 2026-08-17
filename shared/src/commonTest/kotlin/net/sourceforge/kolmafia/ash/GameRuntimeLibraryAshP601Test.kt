package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CyberRealmSync

class GameRuntimeLibraryAshP601Test {

    @Test
    fun halfWay_setsZone1To10() {
        val prefs = Preferences(MapSettings())
        assertTrue(CyberRealmSync.applyFromChoice(1545, prefs))
        assertEquals(10, prefs.getInt("_cyberZone1Turns", 0))
    }

    @Test
    fun final_setsZone2To20() {
        val prefs = Preferences(MapSettings())
        assertTrue(CyberRealmSync.applyFromChoice(1548, prefs))
        assertEquals(20, prefs.getInt("_cyberZone2Turns", 0))
    }

    @Test
    fun zone3_halfAndFinal() {
        val prefs = Preferences(MapSettings())
        assertTrue(CyberRealmSync.applyFromChoice(1549, prefs))
        assertEquals(10, prefs.getInt("_cyberZone3Turns", 0))
        assertTrue(CyberRealmSync.applyFromChoice(1550, prefs))
        assertEquals(20, prefs.getInt("_cyberZone3Turns", 0))
    }
}
