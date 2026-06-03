package net.sourceforge.kolmafia.shop

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ShopRequestTest {

    private val successHtml = "<html><body>You acquire an item: <b>stuffed cocoabo</b></body></html>"

    private fun captureClient(): Pair<HttpClient, MutableList<String>> {
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond(successHtml, HttpStatusCode.OK)
        }
        return HttpClient(engine) to bodies
    }

    @Test
    fun buy_sendsCorrectFormFields() = runTest {
        val (client, bodies) = captureClient()
        val request = ShopRequest(client)

        request.buy(shopId = "shore", rowId = 1, quantity = 2)

        val body = bodies[0]
        assertTrue(body.contains("whichshop=shore"), "body: $body")
        assertTrue(body.contains("whichrow=1"), "body: $body")
        assertTrue(body.contains("quantity=2"), "body: $body")
        assertTrue(body.contains("action=buyitem"), "body: $body")
    }

    @Test
    fun buy_returnsSuccess() = runTest {
        val (client, _) = captureClient()
        val result = ShopRequest(client).buy(shopId = "shore", rowId = 1, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun buy_networkError_returnsFailure() = runTest {
        val engine = MockEngine { throw Exception("network error") }
        val result = ShopRequest(HttpClient(engine)).buy("shore", 1, 1)
        assertTrue(result.isFailure)
    }
}
