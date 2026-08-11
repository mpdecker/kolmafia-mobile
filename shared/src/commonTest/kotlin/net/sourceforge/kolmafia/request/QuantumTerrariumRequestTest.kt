package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.QuantumTerrariumSync
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.event.GameEventBus

class QuantumTerrariumRequestTest {

    @BeforeTest
    fun resetCounterGate() {
        QuantumTerrariumRequest.resetLastCheckedForTest()
    }

    @Test
    fun checkCounter_fetchesWhenCounterNotRunning() = runTest {
        var qterrariumCalls = 0
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("qterrarium.php")) {
                qterrariumCalls++
                respond(QT_HTML, HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.OK)
            }
        })
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(path = AscensionPath.QUANTUM_TERRARIUM.apiName, currentrun = "50"),
        )
        QuantumTerrariumRequest.checkCounter(
            client = client,
            character = char,
            preferences = prefs,
            url = "adventure.php",
            hasResult = true,
        )
        assertEquals(1, qterrariumCalls)
    }

    @Test
    fun checkCounter_skipsWhenCounterStillActive() = runTest {
        var qterrariumCalls = 0
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("qterrarium.php")) {
                qterrariumCalls++
            }
            respond("", HttpStatusCode.OK)
        })
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(
            prefs,
            currentRun = 50,
            turns = 5,
            "${QuantumTerrariumSync.FAMILIAR_COUNTER} loc=*",
            "x.gif",
        )
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(path = AscensionPath.QUANTUM_TERRARIUM.apiName, currentrun = "50"),
        )
        QuantumTerrariumRequest.checkCounter(
            client = client,
            character = char,
            preferences = prefs,
            url = "adventure.php",
            hasResult = true,
        )
        assertEquals(0, qterrariumCalls)
    }

    @Test
    fun forceAlign_postsActionFam() = runTest {
        var posted = false
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("qterrarium.php") &&
                request.method == HttpMethod.Post
            ) {
                posted = true
                respond(FORCE_HTML, HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.OK)
            }
        })
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter()
        val logger = SessionLogger(prefs, GameEventBus())
        val result = QuantumTerrariumRequest.forceAlign(
            client = client,
            familiarId = 180,
            character = char,
            preferences = prefs,
            sessionLogger = logger,
        )
        assertTrue(posted)
        assertTrue(result.forcedAlign)
        assertTrue(
            prefs.getString("sessionLogLines", "").contains("Forced next quantum familiar"),
        )
    }

    companion object {
        private val QT_HTML = """
            <i>Your Current Familiar</i><br /><img onClick='fam(263)'><br /><b>Trubastian</b><br /><a href=showplayer.php?who=202148>spOOnge</a>'s Bowlet<br /><br />
        """.trimIndent()

        private val FORCE_HTML = """
            $QT_HTML
            <i>Your Familiar in <b>3</b> Adventures</i><br /><img onClick='fam(180)'><br /><b>Old Elizabeth</b><br><a href=showplayer.php?who=292033>crusader06</a>'s Miniature Sword & Martini Guy<br /><br />
            arranging the quanta to force your desired future
        """.trimIndent()
    }
}
