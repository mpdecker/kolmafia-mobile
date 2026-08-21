package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ElvmachineRequest
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.event.GameEventBus

class GameRuntimeLibraryAshP610Test {

    @Test
    fun revision_phase610() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun slot_logsInsertAndConsumesCard() {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            ElvmachineRequest.parseResponse(
                url = "elvmachine.php?action=slot&whichcard=3151",
                sessionLogger = logger,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(3151 to 1), consumed)
        assertTrue(
            prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
                .contains("Inserting a El Vibrato punchcard (142 holes) into the slot."),
        )
    }

    @Test
    fun button_logsPush() {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        assertTrue(
            ElvmachineRequest.registerRequest(
                url = "elvmachine.php?action=button",
                sessionLogger = logger,
            ),
        )
        assertTrue(prefs.getString(SessionLogger.SESSION_LOG_KEY, "").contains("Pushing the button."))
    }
}
