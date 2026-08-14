package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop DaycareCommand spa path — Boxing Daycare choices 1334/1335. */
class DaycareRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun visitSpa(
        spaOption: Int,
        preferences: Preferences?,
    ): Result<String> {
        if (spaOption !in 1..4) {
            return Result.failure(IllegalArgumentException("Choice not recognised"))
        }
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        if (!prefs.getBoolean("daycareOpen", false) &&
            !prefs.getBoolean("_daycareToday", false)
        ) {
            return Result.failure(IllegalStateException("You need a boxing daycare first."))
        }
        if (prefs.getBoolean(SPA_USED_PREF, false)) {
            return Result.failure(
                IllegalStateException("You have already visited the Boxing Day Spa today"),
            )
        }
        return try {
            val visit = client.submitForm(
                url = "$KOL_BASE_URL/place.php",
                formParameters = parameters {
                    append("whichplace", "town_wrong")
                    append("action", "townwrong_boxingdaycare")
                },
            )
            if (!visit.status.isSuccess()) {
                return Result.failure(IllegalStateException("Daycare visit failed."))
            }
            choiceRequest.choose(ENTER_CHOICE, 2).onFailure { return Result.failure(it) }
            choiceRequest.choose(SPA_CHOICE, spaOption).map { (html, _) ->
                prefs.setBoolean(SPA_USED_PREF, true)
                html
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val SPA_USED_PREF = "_daycareSpa"
        const val ENTER_CHOICE = 1334
        const val SPA_CHOICE = 1335

        fun findSpaOption(parameters: String): Int {
            val p = parameters.trim().lowercase()
            return when {
                p.contains("mus") -> 1
                p.contains("mox") -> 2
                p.contains("mys") -> 3
                p.contains("regen") -> 4
                else -> 0
            }
        }
    }
}
