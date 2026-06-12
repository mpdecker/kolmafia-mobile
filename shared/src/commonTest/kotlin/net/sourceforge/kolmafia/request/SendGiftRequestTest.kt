package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendGiftRequestTest {

    @Test
    fun send_postsTownSendgift() = runTest {
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            respond("Package sent.", HttpStatusCode.OK)
        })
        val result = SendGiftRequest(client).send(
            recipient = "Buffy",
            message = "hi",
            attachments = listOf(MailAttachment(itemId = 42, quantity = 1)),
        )
        assertTrue(result.isSuccess)
        assertTrue(paths.any { it.contains("town_sendgift.php") })
    }

    @Test
    fun smallestFor_picksCapacityMatchingPackage() {
        val pkg = GiftPackages.smallestFor(3)
        assertEquals(3, pkg?.maxCapacity)
        assertEquals(3, pkg?.radio)
    }

    @Test
    fun send_rejectsEmptyAttachments() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val result = SendGiftRequest(client).send("Buffy", "hi", emptyList())
        assertTrue(result.isFailure)
    }
}
