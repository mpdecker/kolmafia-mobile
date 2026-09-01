package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Explicit live submission of queued spading data through KoL's kmail endpoint. */
open class SpadeRequest(
    private val sendMailRequest: SendMailRequest,
    private val preferences: Preferences,
    private val sessionLogger: SessionLogger? = null,
) {
    data class Entry(val contents: String, val recipient: String, val explanation: String)

    data class SubmissionResult(
        val sent: Int,
        val failed: Int,
        val malformed: Int,
        val remaining: Int,
    ) {
        val succeeded: Boolean get() = failed == 0 && malformed == 0
    }

    fun pending(): List<Entry> =
        preferences.getString("spadingData", "")
            .split('|')
            .chunked(3)
            .mapNotNull { fields ->
                if (fields.size != 3 || fields.any(String::isBlank)) null
                else Entry(fields[0], fields[1], fields[2])
            }

    suspend fun submit(): Result<SubmissionResult> {
        val raw = preferences.getString("spadingData", "")
        if (raw.isBlank()) {
            return Result.success(SubmissionResult(0, 0, 0, 0))
        }
        val fields = raw.split('|')
        if (fields.size < 3) {
            return Result.success(SubmissionResult(0, 0, 1, fields.size))
        }

        val remaining = mutableListOf<String>()
        var sent = 0
        var failed = 0
        var malformed = 0
        fields.chunked(3).forEach { entry ->
            if (entry.size != 3 || entry.any(String::isBlank)) {
                malformed++
                remaining += entry
                return@forEach
            }
            val (contents, recipient, _) = entry
            val result = sendMailRequest.send(recipient, contents)
            if (result.isSuccess) {
                sent++
                sessionLogger?.appendRawLine("Spade data submitted to $recipient: $contents")
            } else {
                failed++
                remaining += entry
                sessionLogger?.appendRawLine(
                    "Spade data submission failed for $recipient: " +
                        (result.exceptionOrNull()?.message ?: "request failed"),
                )
            }
        }
        preferences.setString("spadingData", remaining.joinToString("|"))
        return Result.success(SubmissionResult(sent, failed, malformed, remaining.size))
    }
}
