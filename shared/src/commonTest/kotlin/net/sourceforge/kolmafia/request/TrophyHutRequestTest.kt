package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class TrophyHutRequestTest {

    private fun logger(): SessionLogger =
        SessionLogger(Preferences(MapSettings()), GameEventBus())

    @Test
    fun parseResponse_deductsMeatOnSuccessfulInstall() {
        val character = KoLCharacter()
        character.updateMeat(50_000)
        val sessionLogger = logger()
        TrophyHutRequest.parseResponse(
            url = "trophy.php?action=buytrophy&whichtrophy=3",
            html = "Your trophy has been installed at your campsite.",
            character = character,
            sessionLogger = sessionLogger,
        )
        assertEquals(40_000, character.state.value.meat)
        assertTrue(sessionLogger.recentLines().any { it.contains("10,000 Meat") })
    }

    @Test
    fun registerRequest_logsBuyTrophyLine() {
        val sessionLogger = logger()
        val logged = TrophyHutRequest.registerRequest(
            url = "trophy.php?action=buytrophy&whichtrophy=12",
            sessionLogger = sessionLogger,
        )
        assertTrue(logged)
        assertTrue(sessionLogger.recentLines().any { it.contains("Buying trophy #12") })
    }
}
