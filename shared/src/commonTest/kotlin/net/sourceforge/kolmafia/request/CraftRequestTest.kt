package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CraftRequestTest {

    @Test
    fun parseCreatedCount_sumsCraftComments() {
        val html = """
            <!-- cr:2x1,2=99 -->
            <!-- cr:1x3,4=99 -->
        """.trimIndent()
        assertEquals(3, CraftRequest.parseCreatedCount(html))
    }

    @Test
    fun craft_returnsParsedCount() = runTest {
        val client = HttpClient(MockEngine {
            respond("You craft stuff <!-- cr:2x10,11=42 -->", HttpStatusCode.OK)
        })
        assertEquals(2, CraftRequest(client).craft("combine", 2, 10, 11))
    }
}
