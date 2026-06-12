package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Text-only kmail via sendmessage.php (attachments deferred). */
open class SendMailRequest(private val client: HttpClient) {

    open suspend fun send(recipient: String, message: String): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/sendmessage.php",
            formParameters = parameters {
                append("action", "send")
                append("towho", recipient)
                append("message", message)
            },
        )
        if (response.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
