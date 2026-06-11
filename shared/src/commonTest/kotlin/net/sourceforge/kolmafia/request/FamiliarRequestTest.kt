package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.familiar.FamiliarRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FamiliarRequestTest {

    @Test
    fun enthrone_postsHatseatAction() = runTest {
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            respond("", HttpStatusCode.OK)
        })
        FamiliarRequest(client).enthrone(42)
        assertTrue(paths.any { it.contains("familiar.php") })
    }

    @Test
    fun bjornify_postsBackpackAction() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val result = FamiliarRequest(client).bjornify(0)
        assertTrue(result.isSuccess)
    }

    @Test
    fun stealItem_postsStealAction() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        assertTrue(FamiliarRequest(client).stealItem(42).isSuccess)
    }
}
