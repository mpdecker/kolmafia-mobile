package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.LocketRequest] — Combat Lover's Locket reminisce. */
open class LocketRequest(private val client: HttpClient) {

    open suspend fun visit(): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/inventory.php") {
                parameter("reminisce", "1")
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun reminisce(monsterId: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        val opened = visit()
        if (opened.isFailure) return opened
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/choice.php",
                formParameters = parameters {
                    append("whichchoice", CHOICE_ID.toString())
                    append("option", "1")
                    append("mid", monsterId.toString())
                },
            )
            if (response.status.isSuccess()) {
                net.sourceforge.kolmafia.recovery.BetweenBattleInvoker.run(true)
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val LOCKET_ITEM_ID = 10893
        const val CHOICE_ID = 1463
        const val PREF_FOUGHT = "_locketMonstersFought"
        const val LOCKET_NAME = "combat lover's locket"

        fun recordReminisce(preferences: Preferences, monsterId: Int) {
            if (monsterId <= 0) return
            net.sourceforge.kolmafia.session.LocketManager.rememberMonster(monsterId)
            val fought = preferences.getString(PREF_FOUGHT, "")
                .split('|', ',')
                .mapNotNull { it.trim().toIntOrNull() }
                .toMutableList()
            if (monsterId !in fought) fought.add(monsterId)
            preferences.setString(PREF_FOUGHT, fought.joinToString(","))
        }
    }
}
