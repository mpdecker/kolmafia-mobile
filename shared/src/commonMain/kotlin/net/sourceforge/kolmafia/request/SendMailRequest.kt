package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL

data class MailAttachment(val itemId: Int, val quantity: Int)

open class SendMailRequest(private val client: HttpClient) {

    open suspend fun send(
        recipient: String,
        message: String,
        attachments: List<MailAttachment> = emptyList(),
        meat: Long = 0,
    ): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/sendmessage.php",
            formParameters = parameters {
                append("action", "send")
                append("towho", recipient)
                append("message", message)
                if (meat > 0) append("sendmeat", meat.toString())
                attachments.forEachIndexed { index, att ->
                    val n = index + 1
                    append("whichitem$n", att.itemId.toString())
                    append("howmany$n", att.quantity.toString())
                }
            },
        )
        if (response.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Text-only kmail (Phase 31 compatibility). */
    open suspend fun send(recipient: String, message: String): Result<Unit> =
        send(recipient, message, emptyList(), 0)

    companion object {
        const val DEFAULT_MESSAGE =
            "Keep the contents of this message top-sekrit, ultra hush-hush."
        const val MAX_ATTACHMENTS = 11
    }
}
