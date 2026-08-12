package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BastilleBattalionSync
import net.sourceforge.kolmafia.session.BastilleSyncContext
import net.sourceforge.kolmafia.session.SessionLogger

class GameRuntimeLibraryAshP238Test {

    @Test
    fun revision_phase222() {
        assertEquals("phase470", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun registerRequest_logsChargeActionToSessionLog() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_bastilleGameTurn", 7)
        val logger = SessionLogger(prefs, net.sourceforge.kolmafia.event.GameEventBus())
        val context = BastilleSyncContext(sessionLogger = logger, playerId = 42)
        assertTrue(
            BastilleBattalionSync.registerRequest(
                BastilleBattalionSync.CHOICE_CASTLE_VS_CASTLE,
                decision = 1,
                prefs = prefs,
                context = context,
            ),
        )
        val lines = logger.recentLines()
        assertTrue(lines.any { it.contains("Turn #7: Charge!") })
    }
}
