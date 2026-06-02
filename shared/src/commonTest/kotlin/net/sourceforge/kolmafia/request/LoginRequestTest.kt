package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginRequestTest {

    private fun makeClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler)) {
        install(HttpCookies)
        install(HttpRedirect) { checkHttpMethod = false }
    }

    @Test
    fun login_returnsSuccess_onRedirectToMain() = runTest {
        val client = makeClient { request ->
            when (request.url.encodedPath) {
                "/login.php" -> respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(
                        HttpHeaders.Location,
                        "https://www.kingdomofloathing.com/main.php"
                    )
                )
                "/main.php" -> respond("<html>main</html>", HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val result = LoginRequest(client).login("validuser", "password")
        assertTrue(result is LoginResult.Success)
    }

    @Test
    fun login_returnsFailure_onLoginFailedBody() = runTest {
        val client = makeClient { request ->
            if (request.url.encodedPath == "/login.php") {
                respond("<p>Login failed.</p>", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.NotFound)
            }
        }
        val result = LoginRequest(client).login("baduser", "wrongpassword")
        assertTrue(result is LoginResult.Failure)
        assertEquals("Invalid username or password", (result as LoginResult.Failure).message)
    }

    @Test
    fun login_returnsError_onServerError() = runTest {
        val client = makeClient { respond("", HttpStatusCode.InternalServerError) }
        val result = LoginRequest(client).login("user", "pass")
        assertTrue(result is LoginResult.Failure || result is LoginResult.Error)
    }
}
