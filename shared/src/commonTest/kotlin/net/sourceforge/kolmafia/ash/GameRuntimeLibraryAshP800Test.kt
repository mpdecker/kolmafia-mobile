package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.SausageGrinderChoiceSync

class GameRuntimeLibraryAshP800Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesSausageCounters() {
        val prefs = Preferences(MapSettings())
        val html =
            """grinder needs 50 of the 222 required units of filling to make a sausage.  Your grinder reads "172" units."""
        assertTrue(
            SausageGrinderChoiceSync.applyVisit(
                choiceId = 1339,
                html = html,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt("_sausagesMade", -1))
        assertEquals("172", prefs.getString("sausageGrinderUnits", ""))
    }

    @Test
    fun visit_parsesCommaRequired() {
        val prefs = Preferences(MapSettings())
        val html =
            """grinder needs 10 of the 1,110 required units of filling to make a sausage.  Your grinder reads "5" units."""
        assertTrue(
            SausageGrinderChoiceSync.applyVisit(
                choiceId = 1339,
                html = html,
                preferences = prefs,
            ),
        )
        assertEquals(9, prefs.getInt("_sausagesMade", -1))
        assertEquals("5", prefs.getString("sausageGrinderUnits", ""))
    }
}
