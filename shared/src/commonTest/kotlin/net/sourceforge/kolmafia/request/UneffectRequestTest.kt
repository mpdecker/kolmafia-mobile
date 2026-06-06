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
}
