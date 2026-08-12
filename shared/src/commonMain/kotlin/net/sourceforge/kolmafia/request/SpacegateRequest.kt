package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop SpacegateCommand vaccine path — vaccinator + choice 1234. */
class SpacegateRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun takeVaccine(
        vaccine: Int,
        preferences: Preferences?,
    ): Result<String> {
        if (vaccine !in 1..3) {
            return Result.failure(IllegalArgumentException("Choose vaccine 1, 2, or 3"))
        }
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        if (!prefs.getBoolean("spacegateAlways", false) &&
            !prefs.getBoolean("_spacegateToday", false)
        ) {
            return Result.failure(
                IllegalStateException("You are not cleared to access the Spacegate facility"),
            )
        }
        if (prefs.getBoolean(VACCINE_USED_PREF, false)) {
            return Result.failure(
                IllegalStateException("You've already been vaccinated today"),
            )
        }
        return try {
            val visit = client.submitForm(
                url = "$KOL_BASE_URL/place.php",
                formParameters = parameters {
                    append("whichplace", "spacegate")
                    append("action", "sg_vaccinator")
                },
            )
            if (!visit.status.isSuccess()) {
                return Result.failure(IllegalStateException("Spacegate vaccinator visit failed."))
            }
            // Desktop parses unlock prefs from visit HTML; honor existing prefs this phase.
            if (!prefs.getBoolean("spacegateVaccine$vaccine", false)) {
                return Result.failure(
                    IllegalStateException("You have not unlocked that vaccine yet"),
                )
            }
            choiceRequest.choose(CHOICE_ID, vaccine).map { (html, _) ->
                prefs.setBoolean(VACCINE_USED_PREF, true)
                html
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val VACCINE_USED_PREF = "_spacegateVaccine"
        const val CHOICE_ID = 1234

        fun parseVaccine(parameters: String): Int {
            val parts = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (parts.isEmpty()) return 0
            if (!parts[0].equals("vaccine", ignoreCase = true)) return 0
            val n = parts.getOrNull(1)?.toIntOrNull() ?: return 0
            return if (n in 1..3) n else 0
        }
    }
}
