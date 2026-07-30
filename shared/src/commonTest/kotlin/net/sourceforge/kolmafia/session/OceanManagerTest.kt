package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.OceanDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.OceanRequest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OceanManagerTest {

    private lateinit var preferences: Preferences

    @BeforeTest
    fun setUp() {
        OceanDatabase.resetForTest()
        val gilligan = OceanDatabase.OceanPoint(12, 84)
        val mainland = OceanDatabase.OceanPoint(12, 12)
        val plinth = OceanDatabase.OceanPoint(63, 29)
        OceanDatabase.injectForTest(
            OceanDatabase.parseForTest(
                """
                12	84	Gilligan's Island
                12	12	mainland
                63	29	Plinth
                """.trimIndent(),
            ),
        )
        preferences = Preferences(MapSettings())
    }

    @AfterTest
    fun tearDown() {
        OceanDatabase.resetForTest()
    }

    @Test
    fun getDestination_manualReturnsNull() {
        preferences.setString("oceanDestination", "manual")
        assertNull(OceanManager.getDestination(preferences))
    }

    @Test
    fun getDestination_coordParsesLonLat() {
        preferences.setString("oceanDestination", "63,29")
        assertEquals(OceanDatabase.OceanPoint(63, 29), OceanManager.getDestination(preferences))
    }

    @Test
    fun getDestination_muscleReturnsGilliganPoint() {
        preferences.setString("oceanDestination", "muscle")
        val point = OceanManager.getDestination(preferences)
        assertNotNull(point)
        assertEquals(OceanDatabase.OceanDestination.GILLIGAN, OceanDatabase.destinationAt(point))
    }

    @Test
    fun shouldAutomate_falseForManualAndIgnore() {
        preferences.setString("oceanDestination", "manual")
        assertFalse(OceanManager.shouldAutomate(preferences))
        preferences.setString("oceanDestination", "ignore")
        assertFalse(OceanManager.shouldAutomate(preferences))
    }

    @Test
    fun shouldAutomate_trueForKeywordDestination() {
        preferences.setString("oceanDestination", "sphere")
        assertTrue(OceanManager.shouldAutomate(preferences))
    }

    @Test
    fun processOceanAdventure_mainlandRerollsAndContinues() = runTest {
        preferences.setString("oceanDestination", "12,12")
        preferences.setString("oceanAction", "continue")
        val logs = mutableListOf<String>()
        val client = HttpClient(MockEngine { respond("<html>sailed</html>", HttpStatusCode.OK) })
        val request = OceanRequest(client)

        val result = OceanManager.processOceanAdventure(
            request,
            preferences,
            log = logs::add,
        )

        assertIs<OceanManager.OceanResult.Continued>(result)
        assertEquals("<html>sailed</html>", result.html)
        assertTrue(logs.any { it.contains("You cannot sail to the mainland.") })
    }

    @Test
    fun processOceanAdventure_stopActionReturnsStop() = runTest {
        preferences.setString("oceanDestination", "63,29")
        preferences.setString("oceanAction", "stop")
        val client = HttpClient(MockEngine { respond("<html>sailed</html>", HttpStatusCode.OK) })
        val request = OceanRequest(client)

        val result = OceanManager.processOceanAdventure(request, preferences)
        assertIs<OceanManager.OceanResult.Stop>(result)
        assertEquals("Stop", result.message)
    }

    @Test
    fun processOceanAdventure_manualDestinationReturnsManual() = runTest {
        preferences.setString("oceanDestination", "manual")
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val request = OceanRequest(client)

        val result = OceanManager.processOceanAdventure(request, preferences)
        assertIs<OceanManager.OceanResult.Manual>(result)
        assertEquals("Pick a valid course.", result.message)
    }

    @Test
    fun registerRequest_logsIntroEncounter() {
        val logger = SessionLogger(preferences, GameEventBus())
        OceanManager.registerRequest("ocean.php?intro=1", logger)
        assertTrue(
            preferences.getString(SessionLogger.SESSION_LOG_KEY, "")
                .contains("Encounter: Set an Open Course for the Virgin Booty"),
        )
    }

    @Test
    fun registerRequest_logsSailDestination() {
        val logger = SessionLogger(preferences, GameEventBus())
        OceanManager.registerRequest("ocean.php?lon=63&lat=29", logger)
        assertTrue(
            preferences.getString(SessionLogger.SESSION_LOG_KEY, "")
                .contains("Setting sail for (63,29) = Plinth"),
        )
    }
}
