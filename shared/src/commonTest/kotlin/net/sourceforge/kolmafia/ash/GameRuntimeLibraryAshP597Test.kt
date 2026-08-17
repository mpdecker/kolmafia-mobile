package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CyberRealmSync

class GameRuntimeLibraryAshP597Test {

    @Test
    fun alreadyHacked_setsZoneTurns() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CyberRealmSync.applyFromAdventure(
                adventureId = "585",
                html = "You've already hacked this system.",
                preferences = prefs,
            ),
        )
        assertEquals(20, prefs.getInt("_cyberZone1Turns", 0))
    }

    @Test
    fun chipDrawer_setsDatastick() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CyberRealmSync.applyFromServerRoom(
                url = "place.php?whichplace=serverroom&action=serverroom_chipdrawer",
                html = "chip",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("cyberDatastickCollected", false))
    }

    @Test
    fun fileDrawer_parsesZones() {
        val prefs = Preferences(MapSettings())
        val html = """
            <b>Owner:</b> Acme Corp<br>Security Level: 2<br>Countermeasures: firewall<br>Active Intrusion: blackhat<br>
        """.trimIndent()
        assertTrue(
            CyberRealmSync.applyFromServerRoom(
                url = "place.php?whichplace=serverroom&action=serverroom_filedrawer",
                html = html,
                preferences = prefs,
            ),
        )
        assertEquals("Acme Corp", prefs.getString("_cyberZone2Owner", ""))
        assertEquals("firewall", prefs.getString("_cyberZone2Defense", ""))
        assertEquals("greyhat hacker", prefs.getString("_cyberZone2Hacker", ""))
    }

    @Test
    fun monorail_setsCrToday() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CyberRealmSync.applyFromMonorail(
                url = "place.php?whichplace=monorail",
                html = "Server Room entrance",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_crToday", false))
    }
}
