package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop HeyDezeRequest styxbuff — Bad Moon Styx Pixie. */
class HeyDezeRequest(
    private val client: HttpClient,
) {
    suspend fun takeBuff(
        whichBuff: Int,
        preferences: Preferences?,
        charState: CharacterState?,
    ): Result<String> {
        if (!isBadMoon(charState)) {
            return Result.failure(
                IllegalStateException("You can't find the Styx unless you are in Bad Moon."),
            )
        }
        if (whichBuff !in BUFF_IDS) {
            return Result.failure(
                IllegalArgumentException("You can only buff muscle, mysticality, or moxie."),
            )
        }
        if (preferences?.getBoolean(VISITED_PREF, false) == true) {
            return Result.failure(
                IllegalStateException("You can only visit the Styx Pixie once a day."),
            )
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/heydeze.php",
                formParameters = parameters {
                    append("action", "styxbuff")
                    append("whichbuff", whichBuff.toString())
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Styx visit failed."))
            }
            val html = response.bodyAsText()
            if (html.isEmpty()) {
                return Result.failure(IllegalStateException("You can't find the Styx Pixie."))
            }
            if (html.contains("already got a buff today", ignoreCase = true)) {
                preferences?.setBoolean(VISITED_PREF, true)
                return Result.failure(
                    IllegalStateException("You can only visit the Styx Pixie once a day."),
                )
            }
            parseResponse("action=styxbuff&whichbuff=$whichBuff", html, preferences)
            Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val VISITED_PREF = "styxPixieVisited"
        val BUFF_IDS = setOf(446, 447, 448)

        fun findBuffId(tag: String): Int {
            val t = tag.trim().lowercase()
            return when {
                t.startsWith("mus") -> 446
                t.startsWith("mys") -> 447
                t.startsWith("mox") -> 448
                else -> 0
            }
        }

        fun isBadMoon(charState: CharacterState?): Boolean =
            ZodiacSign.find(charState?.zodiacSign.orEmpty())?.isBadMoon == true ||
                charState?.zodiacSign.equals("Bad Moon", ignoreCase = true) == true

        fun parseResponse(url: String, html: String, preferences: Preferences?) {
            if (preferences == null) return
            if (!url.contains("action=styxbuff", ignoreCase = true)) return
            preferences.setBoolean(VISITED_PREF, true)
        }
    }
}
