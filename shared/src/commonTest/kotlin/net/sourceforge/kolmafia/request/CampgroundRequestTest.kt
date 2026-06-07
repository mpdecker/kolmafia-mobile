package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CampgroundRequestTest {

    private fun mockClientCapturing(): Pair<HttpClient, MutableList<String>> {
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("ok")
        }
        return HttpClient(engine) to bodies
    }

    @Test
    fun harvestGarden_success_returnsSuccess() = runTest {
        val client = HttpClient(MockEngine { respond("ok") })
        val result = CampgroundRequest(client).harvestGarden()
        assertTrue(result.isSuccess)
    }

    @Test
    fun harvestGarden_networkError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { throw Exception("timeout") })
        val result = CampgroundRequest(client).harvestGarden()
        assertTrue(result.isFailure)
    }

    @Test
    fun harvestGarden_serverError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("err", HttpStatusCode.InternalServerError) })
        val result = CampgroundRequest(client).harvestGarden()
        assertTrue(result.isFailure)
    }

    @Test
    fun harvestGarden_sendsCorrectParams() = runTest {
        val (client, bodies) = mockClientCapturing()
        CampgroundRequest(client).harvestGarden()
        val body = bodies[0]
        assertTrue(body.contains("action=garden"), "body=$body")
    }
}
