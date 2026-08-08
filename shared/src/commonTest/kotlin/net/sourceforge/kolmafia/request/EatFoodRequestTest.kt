package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.session.ConsumptionHelperState

class EatFoodRequestTest {

    @AfterTest
    fun tearDown() {
        ConsumptionHelperState.resetForTest()
    }

    private fun makeClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler))

    @Test
    fun eat_callsCorrectEndpoint() = runTest {
        val capturedEncodedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedEncodedPaths += request.url.encodedPath
            respond(
                content = "<html>You eat the food.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        EatFoodRequest(client).eat(itemId = 1, quantity = 1)
        assertTrue(
            capturedEncodedPaths.any { it == "/inv_eat.php" },
            "Expected endpoint /inv_eat.php but got: $capturedEncodedPaths"
        )
    }

    @Test
    fun eat_sendsCorrectItemId() = runTest {
        val capturedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            respond(
                content = "<html>You eat the food.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        EatFoodRequest(client).eat(itemId = 42, quantity = 1)
        assertTrue(
            capturedPaths.any { it.contains("whichitem=42") },
            "Expected whichitem=42 in request path but got: $capturedPaths"
        )
    }

    @Test
    fun eat_returnsSuccessOnOkResponse() = runTest {
        val client = makeClient {
            respond(
                content = "<html>You eat the food.</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        val result = EatFoodRequest(client).eat(itemId = 1, quantity = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun eat_returnsFailureOnServerError() = runTest {
        val client = makeClient { respond("", HttpStatusCode.InternalServerError) }
        val result = EatFoodRequest(client).eat(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun eat_returnsFailureOnNetworkError() = runTest {
        val client = makeClient { throw Exception("Network error") }
        val result = EatFoodRequest(client).eat(itemId = 1, quantity = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun consumeFood_abortsOnTooFullResponse() = runTest {
        val client = makeClient {
            respond("You're too full to eat that.", HttpStatusCode.OK)
        }
        val outcome = EatFoodRequest(client).consumeFood(itemId = 1, quantity = 1).getOrThrow()
        assertTrue(outcome is ConsumptionRequestOutcome.Aborted)
        assertEquals(0, (outcome as ConsumptionRequestOutcome.Aborted).consumed)
    }

    @Test
    fun consumeFood_sendsUtensilWhenFoodHelperQueued() = runTest {
        ConsumptionHelperState.queueFoodHelper(5459, 1)
        val capturedPaths = mutableListOf<String>()
        val client = makeClient { request ->
            capturedPaths += request.url.fullPath
            respond("You eat the food.", HttpStatusCode.OK)
        }
        EatFoodRequest(client).consumeFood(itemId = 100, quantity = 1).getOrThrow()
        assertTrue(capturedPaths.any { it.contains("utensil=5459") })
    }

    @Test
    fun consumeFood_multiIteratesBlackPudding() = runTest {
        var calls = 0
        val client = makeClient {
            calls++
            if (calls == 1) {
                respond("You eat the pudding.", HttpStatusCode.OK)
            } else {
                respond("You're too full to eat that.", HttpStatusCode.OK)
            }
        }
        val outcome = EatFoodRequest(client).consumeFood(itemId = 2338, quantity = 3).getOrThrow()
        assertTrue(outcome is ConsumptionRequestOutcome.Aborted)
        assertEquals(1, (outcome as ConsumptionRequestOutcome.Aborted).consumed)
        assertEquals(2, calls)
    }
}
