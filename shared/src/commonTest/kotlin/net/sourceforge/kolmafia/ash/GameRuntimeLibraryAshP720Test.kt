package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.PeeVPeeRequest
import net.sourceforge.kolmafia.session.PvpManager
import net.sourceforge.kolmafia.session.SessionLogger

class GameRuntimeLibraryAshP720Test {

    @BeforeTest
    fun reset() {
        PvpManager.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        PvpManager.resetForTest()
    }

    private fun logger(): Pair<Preferences, SessionLogger> {
        val prefs = Preferences(MapSettings())
        return prefs to SessionLogger(prefs, GameEventBus())
    }

    private fun log(prefs: Preferences): String =
        prefs.getString(SessionLogger.SESSION_LOG_KEY, "")

    @Test
    fun revision_phase826() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun registerRequest_rulesAndLogsClaimedWithoutLine() {
        val (prefs, logger) = logger()
        assertTrue(PeeVPeeRequest.registerRequest("peevpee.php?place=rules", logger))
        assertTrue(PeeVPeeRequest.registerRequest("peevpee.php?place=logs", logger))
        assertTrue(PeeVPeeRequest.registerRequest("peevpee.php?place=boards", logger))
        assertTrue(PeeVPeeRequest.registerRequest("peevpee.php", logger))
        assertEquals("", log(prefs))
    }

    @Test
    fun registerRequest_fightUrlLogsAttackLine() {
        PvpManager.parseStances(
            """<select name="stance"><option value="1" selected>Beary Famous</option></select>""",
        )
        val (prefs, logger) = logger()
        assertTrue(
            PeeVPeeRequest.registerRequest(
                "peevpee.php?action=fight&place=fight&attacktype=flowers&ranked=1&stance=1&who=",
                logger,
            ),
        )
        assertTrue(log(prefs).contains("Attack a random opponent for flowers via Beary Famous"))
    }

    @Test
    fun registerRequest_unknownPlaceNotClaimed() {
        val (prefs, logger) = logger()
        assertFalse(PeeVPeeRequest.registerRequest("peevpee.php?place=mystery&action=foo", logger))
        assertFalse(PeeVPeeRequest.registerRequest("adventure.php?snarfblat=1", logger))
        assertEquals("", log(prefs))
    }
}
