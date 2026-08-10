package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.preferences.Preferences

class ModeableRequestTest {

    private fun formParam(body: String, key: String): String? =
        Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)

    @Test
    fun setMode_umbrella_postsInvUseAndChoice() = runTest {
        val requests = mutableListOf<Pair<HttpMethod, String>>()
        val engine = MockEngine { request ->
            requests += request.method to request.url.toString()
            if (request.method == HttpMethod.Post) {
                requests += request.method to request.body.toByteArray().decodeToString()
            }
            respond("ok", HttpStatusCode.OK)
        }
        val prefs = Preferences(MapSettings())
        val request = ModeableRequest(
            client = HttpClient(engine),
            choiceRequest = ChoiceRequest(HttpClient(engine)),
            preferences = prefs,
        )
        val result = request.setMode(Modeable.UMBRELLA, "bucket style")
        assertTrue(result.isSuccess)
        assertTrue(requests.any { it.second.contains("action=useumbrella") })
        val choiceBody = requests.firstOrNull { it.first == HttpMethod.Post && it.second.contains("whichchoice") }?.second
        assertEquals("1466", choiceBody?.let { formParam(it, "whichchoice") })
        assertEquals("3", choiceBody?.let { formParam(it, "option") })
        assertEquals("bucket style", prefs.getString("umbrellaState", ""))
    }

    @Test
    fun setMode_parka_normalizesAliasAndWritesPref() = runTest {
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val prefs = Preferences(MapSettings())
        val request = ModeableRequest(
            client = HttpClient(engine),
            choiceRequest = ChoiceRequest(HttpClient(engine)),
            preferences = prefs,
        )
        val result = request.setMode(Modeable.PARKA, "ml")
        assertTrue(result.isSuccess)
        assertEquals("spikolodon", prefs.getString("parkaMode", ""))
    }

    @Test
    fun normalizeUmbrellaParameter_mapsShorthands() {
        assertEquals("forward-facing", ModeableRequest.normalizeUmbrellaParameter("dr"))
        assertEquals("bucket style", ModeableRequest.normalizeUmbrellaParameter("item"))
    }
}
