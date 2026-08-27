package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.session.CargoPocketSync

/** Desktop [CargoCultistShortsRequest] — cargo cultist shorts pocket automation. */
open class CargoCultistShortsRequest(
    private val client: HttpClient,
) {

    open suspend fun openPockets(): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/inventory.php?action=pocket")
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun submitChoice(
        option: Int,
        pocket: Int = 0,
    ): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/choice.php",
            formParameters = parameters {
                append("whichchoice", CargoPocketSync.CARGO_CULT_CHOICE.toString())
                append("option", option.toString())
                if (pocket != 0) {
                    append("pocket", pocket.toString())
                }
            },
        )
        if (response.status.isSuccess()) {
            Result.success(response.bodyAsText())
        } else {
            Result.failure(Exception("HTTP ${response.status.value}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun inspect(): Result<String> {
        openPockets().onFailure { return Result.failure(it) }
        return submitChoice(option = 2)
    }

    open suspend fun pickPocket(pocket: Int): Result<String> {
        openPockets().onFailure { return Result.failure(it) }
        return submitChoice(option = 1, pocket = pocket).also {
            if (it.isSuccess) {
                net.sourceforge.kolmafia.recovery.BetweenBattleInvoker.run(true)
            }
        }
    }
}
