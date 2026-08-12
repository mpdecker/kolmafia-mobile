package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.data.ConcoctionMayoQueue
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.NpcShopSync

/** Desktop MayosoakCommand — shop.php whichshop=mayoclinic&action=bacta. */
class MayoSoakRequest(
    private val client: HttpClient,
) {
    suspend fun soak(preferences: Preferences?): Result<String> {
        val preflight = preflightError(preferences)
        if (preflight != null) {
            return Result.failure(IllegalStateException(preflight))
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/shop.php",
                formParameters = parameters {
                    append("whichshop", "mayoclinic")
                    append("action", "bacta")
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Mayo soak failed."))
            }
            val html = response.bodyAsText()
            val visitUrl = "$KOL_BASE_URL/shop.php?whichshop=mayoclinic"
            // Refresh soak state from a follow-up visit when possible
            val visit = client.get(visitUrl)
            if (visit.status.isSuccess() && preferences != null) {
                NpcShopSync.applyShopVisit(
                    html = visit.bodyAsText(),
                    url = visitUrl,
                    prefs = preferences,
                    ascensionNumber = preferences.getInt("knownAscensions", 0),
                )
            } else if (preferences != null) {
                preferences.setBoolean(SOAKED_PREF, true)
            }
            Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val SOAKED_PREF = "_mayoTankSoaked"

        fun preflightError(preferences: Preferences?): String? {
            if (!CampgroundItemSync.hasWorkshedItem(preferences, ConcoctionMayoQueue.MAYO_CLINIC)) {
                return "Mayo clinic not installed"
            }
            if (preferences?.getBoolean(SOAKED_PREF, false) == true) {
                return "Already soaked in Mayo tank today"
            }
            return null
        }
    }
}
