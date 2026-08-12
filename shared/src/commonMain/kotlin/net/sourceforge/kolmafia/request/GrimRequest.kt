package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop GrimCommand — familiar.php chatgrim → choice 835. */
class GrimRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun takeBuff(
        option: Int,
        preferences: Preferences?,
        ownsGrimBrother: Boolean,
    ): Result<String> {
        if (option !in 1..3) {
            return Result.failure(IllegalArgumentException("Pick a valid Grim Brother buff"))
        }
        if (!ownsGrimBrother) {
            return Result.failure(IllegalStateException("You don't have a Grim Brother"))
        }
        if (preferences?.getBoolean(BUFF_USED_PREF, false) == true) {
            return Result.failure(
                IllegalStateException("You already received a Grim Brother effect today"),
            )
        }
        return try {
            val chat = client.get("$KOL_BASE_URL/familiar.php") {
                parameter("action", "chatgrim")
            }
            if (!chat.status.isSuccess()) {
                return Result.failure(IllegalStateException("Grim Brother chat failed."))
            }
            choiceRequest.choose(CHOICE_ID, option).map { (html, _) ->
                preferences?.setBoolean(BUFF_USED_PREF, true)
                html
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val BUFF_USED_PREF = "_grimBuff"
        const val CHOICE_ID = 835
        const val GRIM_BROTHER_ID = 179

        fun findOption(parameters: String): Int {
            val p = parameters.trim().lowercase()
            return when {
                p.startsWith("init") || p.startsWith("soles") -> 1
                p.startsWith("hpmp") || p.startsWith("angry") -> 2
                p.startsWith("damage") || p.startsWith("grumpy") -> 3
                else -> 0
            }
        }
    }
}
