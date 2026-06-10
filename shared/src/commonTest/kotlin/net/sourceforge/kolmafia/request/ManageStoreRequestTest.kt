package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ManageStoreRequestTest {

    @Test
    fun addItem_succeedsOnOkResponse() = runTest {
        val req = ManageStoreRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }))
        assertTrue(req.addItem(itemId = 42, price = 1000, limit = 5, quantity = 2).isSuccess)
    }

    @Test
    fun removeItem_succeedsOnOkResponse() = runTest {
        val req = ManageStoreRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }))
        assertTrue(req.removeItem(itemId = 42, quantity = 3).isSuccess)
    }

    @Test
    fun refreshPrices_succeedsOnOkResponse() = runTest {
        val req = ManageStoreRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }))
        assertTrue(req.refreshPrices().isSuccess)
    }
}
