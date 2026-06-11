package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun parseCreatedCount_returnsZeroOnFailureMessage() {
        assertEquals(0, CraftRequest.parseCreatedCount("You can't craft that right now."))
        assertEquals(0, CraftRequest.parseCreatedCount("You don't have enough of the ingredients."))
    }

    @Test
    fun isCraftFailure_detectsKnownSignals() {
        assertTrue(CraftRequest.isCraftFailure("You haven't unlocked that recipe yet."))
        assertFalse(CraftRequest.isCraftFailure("You craft stuff <!-- cr:1x1,2=3 -->"))
    }
}
