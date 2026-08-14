package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.preferences.Preferences

class SpacegateRequestTest {

    @Test
    fun chooseDestination_random_visitsTerminalThenChoice3() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("spacegateAlways", true)
        val result = SpacegateRequest(client, ChoiceRequest(client))
            .chooseDestination("random", prefs)
        assertTrue(result.isSuccess)
        assertTrue(bodies.any { it.contains("action=sg_Terminal") }, bodies.toString())
        assertTrue(
            bodies.any { it.contains("whichchoice=1235") && it.contains("option=3") },
            bodies.toString(),
        )
    }

    @Test
    fun chooseDestination_coords_postsWord() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_spacegateToday", true)
        val result = SpacegateRequest(client, ChoiceRequest(client))
            .chooseDestination("ABCDEFG", prefs)
        assertTrue(result.isSuccess)
        assertTrue(
            bodies.any {
                it.contains("whichchoice=1235") &&
                    it.contains("option=2") &&
                    (it.contains("word=ABCDEFG") || it.contains("word=ABCDEFG".replace("=", "%3D")))
            },
            bodies.toString(),
        )
    }

    @Test
    fun chooseDestination_alreadyChosen_fails() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("spacegateAlways", true)
        prefs.setString(SpacegateRequest.COORDINATES_PREF, "ABCDEFG")
        val result = SpacegateRequest(client, ChoiceRequest(client))
            .chooseDestination("random", prefs)
        assertTrue(result.isFailure)
        assertEquals(
            "You've already chosen a destination today",
            result.exceptionOrNull()?.message,
        )
    }
}
