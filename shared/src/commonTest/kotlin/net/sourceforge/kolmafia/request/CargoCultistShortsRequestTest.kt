package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest

class CargoCultistShortsRequestTest {

    @Test
    fun inspect_postsChoice1420Option2() = runTest {
        val urls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                urls += request.url.toString()
                respond("There appear to be 666 pockets on these shorts.")
            },
        )
        val request = CargoCultistShortsRequest(client)
        val result = request.inspect()
        assertTrue(result.isSuccess)
        assertTrue(urls.any { it.contains("inventory.php?action=pocket") })
        assertTrue(urls.any { it.contains("choice.php") })
    }

    @Test
    fun pickPocket_postsChoiceRequest() = runTest {
        val urls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                urls += request.url.toString()
                respond("Emptied.")
            },
        )
        val request = CargoCultistShortsRequest(client)
        val result = request.pickPocket(373)
        assertTrue(result.isSuccess)
        assertTrue(urls.any { it.contains("inventory.php?action=pocket") })
        assertEquals(1, urls.count { it.contains("choice.php") })
    }
}
