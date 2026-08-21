package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BeachCombManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP844Test {
    @Test
    fun beachCombVisitTracksWalksAndHeadShortcuts() {
        val prefs = Preferences(MapSettings())
        val html = """
            You grab your comb and head to the start of the beach to find a good spot.
            (You have 9 free walks down the beach left today.)
            Visit Beach Head #1 Visit Beach Head #3
        """.trimIndent()
        assertTrue(BeachCombManager.parseCombUsage(html, prefs))
        assertEquals(2, prefs.getInt("_freeBeachWalksUsed", 0))
        assertEquals("1,3", prefs.getString("beachHeadsUnlocked", ""))
        assertEquals("", prefs.getString("_beachHeadsUsed", ""))
    }
}
