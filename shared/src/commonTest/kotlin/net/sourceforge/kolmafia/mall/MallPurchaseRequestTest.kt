package net.sourceforge.kolmafia.mall

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MallPurchaseRequestTest {

    @Test
    fun buy_sendsCorrectFormFields() = runTest {
        val captured = mutableListOf<String>()
        val engine = MockEngine { request ->
            captured += request.body.toByteArray().decodeToString()
            respond("<html>You acquire: fuzzy dice</html>", HttpStatusCode.OK)
        }

        MallPurchaseRequest(HttpClient(engine)).buy(shopId = 12345, itemId = 799, quantity = 1, price = 500L)

        val body = captured[0]
        assertTrue(body.contains("whichstore=12345"), "body: $body")
        assertTrue(body.contains("itemid=799"), "body: $body")
        assertTrue(body.contains("quantity=1"), "body: $body")
        assertTrue(body.contains("ajax=1"), "body: $body")
    }

    @Test
    fun buy_returnsSuccess() = runTest {
        val engine = MockEngine { respond("<html>success</html>", HttpStatusCode.OK) }
        val result = MallPurchaseRequest(HttpClient(engine))
            .buy(shopId = 12345, itemId = 799, quantity = 1, price = 500L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun buy_networkError_returnsFailure() = runTest {
        val engine = MockEngine { throw Exception("network error") }
        val result = MallPurchaseRequest(HttpClient(engine))
            .buy(shopId = 12345, itemId = 799, quantity = 1, price = 500L)
        assertTrue(result.isFailure)
    }
}
