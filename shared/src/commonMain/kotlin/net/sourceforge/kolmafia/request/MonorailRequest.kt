package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop MonorailCommand — Favored by Lyle via place.php monorail_lyle (choice 1309). */
class MonorailRequest(
    private val client: HttpClient,
) {
    suspend fun visitLyle(preferences: Preferences?): Result<String> {
        if (preferences?.getBoolean(FAVORED_PREF, false) == true) {
            return Result.failure(IllegalStateException("You have already had a Lyle buff today"))
        }
        return try {
            val response = client.get(
                "$KOL_BASE_URL/place.php?whichplace=monorail&action=monorail_lyle",
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Monorail visit failed."))
            }
            val html = response.bodyAsText()
            parseResponse(html, preferences)
            Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val FAVORED_PREF = "_lyleFavored"

        fun parseResponse(html: String, preferences: Preferences?) {
            if (preferences == null) return
            // Desktop ChoiceControl case 1309 sets the daily flag when visiting Lyle.
            preferences.setBoolean(FAVORED_PREF, true)
        }
    }
}
