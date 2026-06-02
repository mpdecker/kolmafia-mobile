package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

sealed class LoginResult {
    object Success : LoginResult()
    data class Failure(val message: String) : LoginResult()
    data class Error(val cause: Throwable) : LoginResult()
}

class LoginRequest(private val client: HttpClient) {

    suspend fun login(username: String, password: String): LoginResult {
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/login.php",
                formParameters = parameters {
                    append("loggingin", "Yep.")
                    append("loginname", username)
                    append("password", password)
                    append("secure", "0")
                    append("submitbutton", "Log In")
                }
            )
            val finalUrl = response.request.url.toString()
            val body = response.bodyAsText()
            when {
                finalUrl.contains("main.php") -> LoginResult.Success
                body.contains("Login failed") || body.contains("Invalid password") ->
                    LoginResult.Failure("Invalid username or password")
                else -> LoginResult.Failure("Unexpected response from login server")
            }
        } catch (e: Exception) {
            LoginResult.Error(e)
        }
    }
}
