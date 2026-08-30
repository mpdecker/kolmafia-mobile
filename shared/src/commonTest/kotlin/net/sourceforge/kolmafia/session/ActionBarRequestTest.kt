package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.request.ActionBarRequest
import net.sourceforge.kolmafia.request.LocketRequest

class ActionBarRequestTest {
    @AfterTest
    fun tearDown() {
        ActionBarManager.reset()
    }

    @Test
    fun fetchCachesServerJson() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            respond("""{"bars":[]}""", HttpStatusCode.OK)
        })

        val result = ActionBarRequest(client).fetch()

        assertEquals("""{"bars":[]}""", result.getOrThrow())
        assertEquals("""{"bars":[]}""", ActionBarManager.current())
    }

    @Test
    fun setPostsAndUpdatesCache() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            respond("", HttpStatusCode.OK)
        })

        val result = ActionBarRequest(client).set("""{"bars":[1]}""")

        assertTrue(result.isSuccess)
        assertEquals("""{"bars":[1]}""", ActionBarManager.current())
    }

    @Test
    fun responseParserCachesActionBarResponses() {
        ResponseTextParser.externalUpdate(
            url = "https://www.kingdomofloathing.com/actionbar.php?action=fetch",
            html = """{"bars":[2]}""",
        )

        assertEquals("""{"bars":[2]}""", ActionBarManager.current())
    }

    @Test
    fun locketReminisceRecordsIdsAcrossLegacySeparators() {
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(
            com.russhwolf.settings.MapSettings().apply {
                putString(LocketRequest.PREF_FOUGHT, "101|202")
            },
        )

        LocketRequest.recordReminisce(preferences, 303)

        assertEquals("101,202,303", preferences.getString(LocketRequest.PREF_FOUGHT, ""))
    }
}
