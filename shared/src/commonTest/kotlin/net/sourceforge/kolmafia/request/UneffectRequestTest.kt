package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class UneffectRequestTest {

    @Test
    fun uneffect_success_returnsSuccess() = runTest {
        val client = HttpClient(MockEngine { respond("") })
        val req = UneffectRequest(client)
        val result = req.uneffect(42)
        assertTrue(result.isSuccess)
    }

    @Test
    fun uneffect_networkError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { throw Exception("network error") })
        val req = UneffectRequest(client)
        val result = req.uneffect(42)
        assertTrue(result.isFailure)
    }

    @Test
    fun uneffect_sendsCorrectFormParameters() = runTest {
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("", HttpStatusCode.OK)
        }
        val client = HttpClient(engine)
        UneffectRequest(client).uneffect(42)
        val body = bodies[0]
        assertTrue(body.contains("using=Yep."), "Expected 'using=Yep.' in body: $body")
        assertTrue(body.contains("whicheffect=42"), "Expected 'whicheffect=42' in body: $body")
    }

    @Test
    fun uneffect_serverError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.InternalServerError) })
        val result = UneffectRequest(client).uneffect(42)
        assertTrue(result.isFailure)
    }
}
