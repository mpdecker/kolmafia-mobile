package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.campground.CampAwayAvailability
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [CampAwayRequest] — Getaway campsite cloud-talk buff. */
class CampAwayRequest(
    private val client: HttpClient,
) {
    suspend fun takeCloudBuff(
        preferences: Preferences?,
        charState: CharacterState?,
    ): Result<String> {
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        val state = charState
            ?: return Result.failure(IllegalStateException("Character state is not available."))
        if (!CampAwayAvailability.campAwayTentAvailable(state, prefs)) {
            return Result.failure(IllegalStateException("You need a Getaway Campsite."))
        }
        if (prefs.getInt(CLOUD_BUFFS_PREF, 0) >= 1) {
            return Result.failure(IllegalStateException("Already got a cloud buff today"))
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/place.php",
                formParameters = campAwayForm(SKY),
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Camp Away cloud buff failed."))
            }
            val html = response.bodyAsText()
            parseCloudResponse(html, prefs)
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val SKY = "campaway_sky"
        const val CLOUD_BUFFS_PREF = "_campAwayCloudBuffs"

        internal fun campAwayForm(action: String) = parameters {
            append("whichplace", "campaway")
            append("action", action)
        }

        fun parseCloudResponse(html: String, preferences: Preferences?) {
            val prefs = preferences ?: return
            when {
                html.contains("Cloud-Talk", ignoreCase = true) -> {
                    val used = prefs.getInt(CLOUD_BUFFS_PREF, 0)
                    prefs.setInt(CLOUD_BUFFS_PREF, used + 1)
                }
                else -> {
                    // Desktop sets both when effect parse fails; cloud CLI only needs cloud.
                    prefs.setInt(CLOUD_BUFFS_PREF, 1)
                }
            }
        }
    }
}
