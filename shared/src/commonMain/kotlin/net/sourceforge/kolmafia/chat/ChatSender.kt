package net.sourceforge.kolmafia.chat

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class ChatSender(private val httpClient: HttpClient) {

    suspend fun send(channel: String, message: String): Result<Unit> = try {
        httpClient.submitForm(
            url = "$KOL_BASE_URL/submitnewchat.php",
            formParameters = parameters {
                append("graf", "/$channel $message")
                append("j", "1")
            }
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendPrivate(recipient: String, message: String): Result<Unit> = try {
        httpClient.submitForm(
            url = "$KOL_BASE_URL/submitnewchat.php",
            formParameters = parameters {
                append("graf", "/msg $recipient $message")
                append("j", "1")
            }
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
